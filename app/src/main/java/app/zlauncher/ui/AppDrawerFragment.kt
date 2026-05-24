package app.zlauncher.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.zlauncher.MainViewModel
import app.zlauncher.R
import app.zlauncher.data.AppModel
import app.zlauncher.data.Constants
import app.zlauncher.data.Prefs
import app.zlauncher.databinding.FragmentAppDrawerBinding
import app.zlauncher.helper.deletePinnedShortcut
import app.zlauncher.helper.hideKeyboard
import app.zlauncher.helper.isEinkDisplay
import app.zlauncher.helper.isPrivateSpaceProfile
import app.zlauncher.helper.isSystemApp
import app.zlauncher.helper.openAppInfo
import app.zlauncher.helper.openSearch
import app.zlauncher.helper.openUrl
import app.zlauncher.helper.showKeyboard
import app.zlauncher.helper.showToast
import app.zlauncher.helper.uninstall

class AppDrawerFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked: Boolean = true
    private var currentPrivateSpaceAvailable: Boolean = false

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    companion object {
        /** Set by MainActivity / HomeFragment before switching to this page; consumed once. */
        var pendingFlag: Int = Constants.FLAG_LAUNCH_APP
        var pendingRename: Boolean = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run { ViewModelProvider(this)[MainViewModel::class.java] }
            ?: throw IllegalStateException("Invalid Activity")

        flag = pendingFlag
        canRename = pendingRename
        pendingFlag = Constants.FLAG_LAUNCH_APP
        pendingRename = false

        initSearch()
        initAdapter()
        initObservers()
        initSearchToggle()
        initScrubber()
        applyHintForFlag()
    }

    /** Called when the user navigates back to or re-opens this page with a new pick flag. */
    fun applyFlag(newFlag: Int, canRename: Boolean) {
        this.flag = newFlag
        this.canRename = canRename
        if (_binding == null) return
        adapter.updateFlag(newFlag)
        applyHintForFlag()
        binding.appRename.visibility = View.GONE
        if (newFlag != Constants.FLAG_LAUNCH_APP) {
            binding.search.setQuery("", false)
            showSearchView()
        } else {
            hideSearchView()
        }
    }

    private fun applyHintForFlag() {
        binding.search.queryHint = when {
            flag == Constants.FLAG_HIDDEN_APPS -> getString(R.string.hidden_apps)
            flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_WEATHER_APP -> "Please select an app"
            else -> getString(R.string.search_apps_hint)
        }
        try {
            val searchTextView = binding.search.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
            searchTextView?.gravity = prefs.appLabelAlignment
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.startsWith("!") == true)
                    requireContext().openUrl(Constants.URL_DUCK_SEARCH + query.replace(" ", "%20"))
                else if (adapter.itemCount == 0)
                    openSearch(requireContext())
                else
                    adapter.launchFirstInList()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    adapter.filter.filter(newText)
                    binding.appRename.visibility =
                        if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    binding.scrubber.isVisible = newText.isBlank()
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })

        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }
            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            viewModel.snapToHome()
        }
    }

    private fun initSearchToggle() {
        binding.searchButton.setOnClickListener {
            if (binding.search.isVisible) {
                hideSearchView()
            } else {
                showSearchView()
            }
        }
    }

    private fun showSearchView() {
        binding.search.visibility = View.VISIBLE
        binding.search.isIconified = false
        binding.search.requestFocus()
        binding.search.showKeyboard()
    }

    private fun hideSearchView() {
        binding.search.hideKeyboard()
        binding.search.setQuery("", false)
        binding.search.clearFocus()
        binding.search.visibility = View.GONE
        binding.appRename.visibility = View.GONE
        binding.scrubber.isVisible = true
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            flag,
            prefs.appLabelAlignment,
            appClickListener = { appModel -> handleAppPicked(appModel) },
            appInfoListener = {
                openAppInfo(requireContext(), it.user, it.appPackage)
                viewModel.snapToHome()
            },
            appDeleteListener = { appModel ->
                when (appModel) {
                    is AppModel.PrivateSpaceHeader -> {}
                    is AppModel.PinnedShortcut ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            requireContext().deletePinnedShortcut(
                                packageName = appModel.appPackage,
                                shortcutIdToDelete = appModel.shortcutId,
                                user = appModel.user,
                            )
                        }

                    is AppModel.App -> {
                        if (isPrivateSpaceProfile(requireContext(), appModel.user)) {
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                            requireContext().showToast(getString(R.string.system_app_cannot_delete))
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else {
                            requireContext().uninstall(appModel.appPackage)
                        }
                    }
                }
                viewModel.getAppList()
            },
            appHideListener = { appModel, position ->
                if (appModel is AppModel.PinnedShortcut) {
                    requireContext().showToast("Hiding pinned shortcuts is not supported")
                    return@AppDrawerAdapter
                }
                adapter.appFilteredList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.appsList.remove(appModel)

                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == Constants.FLAG_HIDDEN_APPS) {
                    newSet.remove(appModel.appPackage)
                    newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                } else {
                    newSet.add(appModel.appPackage + "|" + appModel.user.toString())
                }
                prefs.hiddenApps = newSet
                if (newSet.isEmpty()) viewModel.snapToHome()
                if (prefs.firstHide) {
                    binding.search.hideKeyboard()
                    prefs.firstHide = false
                }
                viewModel.getAppList()
                viewModel.getHiddenApps()
            },
            appRenameListener = { appModel, renameLabel ->
                val identifier = when (appModel) {
                    is AppModel.PinnedShortcut -> appModel.shortcutId
                    is AppModel.App -> appModel.appPackage
                    else -> return@AppDrawerAdapter
                }
                prefs.setAppRenameLabel(identifier, renameLabel)
                viewModel.getAppList()
            },
            privateSpaceToggleListener = { viewModel.togglePrivateSpaceLock() },
            privateSpaceSettingsListener = {
                viewModel.openPrivateSpaceSettings()
                viewModel.snapToHome()
            },
        )

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    viewModel.snapToHome()
                }
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay().not()) {
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
        }
    }

    private fun initObservers() {
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let { adapter.setAppList(it.toMutableList()) }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                currentAppList = it
                updateCombinedAppList()
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                viewModel.privateSpaceAvailable.observe(viewLifecycleOwner) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceLocked.observe(viewLifecycleOwner) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceApps.observe(viewLifecycleOwner) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val combined = apps.toMutableList()

        if (flag == Constants.FLAG_LAUNCH_APP && currentPrivateSpaceAvailable) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        adapter.setAppList(combined)
        refreshScrubberLetters(combined)
        adapter.filter.filter(binding.search.query)
    }

    private fun refreshScrubberLetters(items: List<AppModel>) {
        val letters = items
            .filter { it !is AppModel.PrivateSpaceHeader && it.appLabel.isNotBlank() }
            .map { it.appLabel.first().uppercaseChar() }
            .filter { it.isLetter() || it.isDigit() }
            .distinct()
            .sorted()
        binding.scrubber.setLetters(letters)
        binding.scrubber.isVisible = letters.isNotEmpty() && binding.search.query.isNullOrBlank()
    }

    private fun initScrubber() {
        binding.scrubber.onLetterSelected = { letter ->
            val position = adapter.appFilteredList.indexOfFirst {
                it !is AppModel.PrivateSpaceHeader &&
                        it.appLabel.firstOrNull()?.uppercaseChar() == letter
            }
            if (position >= 0) {
                linearLayoutManager.scrollToPositionWithOffset(position, 0)
                binding.scrubberPreview.text = letter.toString()
            }
        }
        binding.scrubber.onScrubStarted = {
            binding.search.hideKeyboard()
            binding.scrubberPreview.visibility = View.VISIBLE
        }
        binding.scrubber.onScrubEnded = {
            binding.scrubberPreview.visibility = View.GONE
        }
    }

    private fun handleAppPicked(appModel: AppModel) {
        viewModel.selectedApp(appModel, flag)
        binding.search.hideKeyboard()
        viewModel.snapToHome()
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) binding.search.hideKeyboard()
            }
        }
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
