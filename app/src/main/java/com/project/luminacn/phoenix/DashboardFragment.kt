package com.project.luminacn.phoenix

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.luminacn.R
import com.project.luminacn.constructors.Account
import com.project.luminacn.constructors.AccountManager
import com.project.luminacn.databinding.FragmentDashboardBinding
import com.project.luminacn.databinding.ItemAccountBinding
import com.project.luminacn.overlay.manager.ConnectionInfoOverlay
import com.project.luminacn.overlay.mods.NotificationType
import com.project.luminacn.overlay.mods.SimpleOverlayNotification
import com.project.luminacn.pack.PackSelectionManager
import com.project.luminacn.router.main.HomeScreen
import com.project.luminacn.service.Services
import com.project.luminacn.util.InjectNeko
import com.project.luminacn.util.MCPackUtils
import com.project.luminacn.util.ServerInit
import com.project.luminacn.viewmodel.MainScreenViewModel
import kotlinx.coroutines.*

class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding
    private lateinit var viewModel: MainScreenViewModel
    private lateinit var accountAdapter: AccountAdapter
    private var isLaunchingMinecraft = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MainScreenViewModel::class.java)
        setupAccountList()
        setupButtonListeners()
        observeViewModel()
        setupComposeView()
    }

    private fun setupComposeView() {
        binding.composeContainer.setContent {
            HomeScreen(
                onStartToggle = {
                    if (Services.isActive) {
                        Services.stop()
                    } else {
                        Services.start()
                        if (!isLaunchingMinecraft) {
                            isLaunchingMinecraft = true
                            launchMinecraft()
                        }
                    }
                    updateButtonVisibility()
                }
            )
        }
    }

    private fun setupAccountList() {
        accountAdapter = AccountAdapter { account ->
            AccountManager.selectAccount(account)
            updateAccountUI()
        }
        binding.accountList.layoutManager = LinearLayoutManager(context)
        binding.accountList.adapter = accountAdapter
    }

    private fun setupButtonListeners() {
        binding.startButton.setOnClickListener {
            if (!Services.isActive) {
                Services.start()
                updateButtonVisibility()
                launchMinecraft()
            }
        }
        binding.stopButton.setOnClickListener {
            if (Services.isActive) {
                Services.stop()
                updateButtonVisibility()
            }
        }
    }

    private fun updateButtonVisibility() {
        binding.startButton.visibility = if (Services.isActive) View.GONE else View.VISIBLE
        binding.stopButton.visibility = if (Services.isActive) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        viewModel.captureModeModel.observe(viewLifecycleOwner) { model ->
            if (model.serverHostName.isNotBlank()) {
                binding.serverInfoContainer.visibility = View.VISIBLE
                binding.serverName.text = model.serverHostName
                binding.serverPort.text = getString(R.string.port, model.serverPort)
            } else {
                binding.serverInfoContainer.visibility = View.GONE
            }
        }

        AccountManager.accounts.observe(viewLifecycleOwner) { accounts ->
            accountAdapter.submitList(accounts)
            updateAccountUI()
        }
    }

    private fun updateAccountUI() {
        val currentAccount = AccountManager.currentAccount
        if (currentAccount != null) {
            binding.accountInfoContainer.visibility = View.VISIBLE
            binding.accountList.visibility = View.GONE
            binding.accountName.text = currentAccount.remark
        } else if (!AccountManager.accounts.value.isNullOrEmpty()) {
            binding.accountInfoContainer.visibility = View.GONE
            binding.accountList.visibility = View.VISIBLE
        } else {
            binding.accountInfoContainer.visibility = View.GONE
            binding.accountList.visibility = View.GONE
        }
    }

    private fun launchMinecraft() {
        val prefs = requireContext().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val injectNekoPack = prefs.getBoolean("injectNekoPackEnabled", false)

        CoroutineScope(Dispatchers.IO).launch {
            delay(2500)
            if (!Services.isActive) {
                isLaunchingMinecraft = false
                return@launch
            }

            val selectedGame = viewModel.selectedGame.value
            if (selectedGame != null) {
                val intent = requireContext().packageManager.getLaunchIntentForPackage(selectedGame)
                if (intent != null && Services.isActive) {
                    withContext(Dispatchers.Main) {
                        startActivity(intent)
                    }

                    delay(3000)
                    if (Services.isActive) {
                        val disableOverlay = prefs.getBoolean("disableConnectionInfoOverlay", false)
                        if (!disableOverlay) {
                            val ip = ConnectionInfoOverlay.getLocalIpAddress(requireContext())
                            ConnectionInfoOverlay.show(ip)
                        }
                    }

                    isLaunchingMinecraft = false

                    try {
                        when {
                            injectNekoPack && PackSelectionManager.selectedPack != null -> {
                                val pack = PackSelectionManager.selectedPack!!
                                val dialog = ProgressDialogFragment().apply {
                                    setCurrentPackName(pack.name)
                                }
                                dialog.show(parentFragmentManager, "ProgressDialog")

                                MCPackUtils.downloadAndOpenPack(requireContext(), pack) { progress ->
                                    dialog.updateProgress(progress)
                                }

                                dialog.dismiss()
                            }
                            injectNekoPack -> {
                                InjectNeko.injectNeko(requireContext()) {}
                            }
                            selectedGame == "com.mojang.minecraftpe" -> {
                                val ip = ConnectionInfoOverlay.getLocalIpAddress(requireContext())
                                ServerInit.addMinecraftServer(requireContext(), ip)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            SimpleOverlayNotification.show("错误: ${e.message}", NotificationType.ERROR, 5000)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        SimpleOverlayNotification.show("游戏启动失败", NotificationType.ERROR, 5000)
                    }
                    isLaunchingMinecraft = false
                }
            } else {
                isLaunchingMinecraft = false
            }
        }
    }

    inner class AccountAdapter(private val onClick: (Account) -> Unit) :
        RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

        private var accounts = listOf<Account>()

        inner class AccountViewHolder(private val binding: ItemAccountBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(account: Account) {
                binding.accountName.text = account.remark
                binding.root.setOnClickListener { onClick(account) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
            val binding = ItemAccountBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return AccountViewHolder(binding)
        }

        override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
            holder.bind(accounts[position])
        }

        override fun getItemCount() = accounts.size

        fun submitList(newList: List<Account>) {
            accounts = newList
            notifyDataSetChanged()
        }
    }

    class ProgressDialogFragment : DialogFragment() {
        private var currentPackName = ""
        private var progress = 0f

        fun setCurrentPackName(name: String) {
            currentPackName = name
        }

        fun updateProgress(value: Float) {
            progress = value
            view?.findViewById<ProgressBar>(R.id.progressBar)?.progress = (progress * 100).toInt()
            view?.findViewById<TextView>(R.id.downloadText)?.text = "正在下载: $currentPackName"
            view?.findViewById<TextView>(R.id.percentageText)?.text = "${(progress * 100).toInt()}%"
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.dialog_download_progress, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            updateProgress(progress)
        }
    }
}
