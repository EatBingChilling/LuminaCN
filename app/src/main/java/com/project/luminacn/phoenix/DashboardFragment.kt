package com.project.luminacn.phoenix

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.project.luminacn.constructors.Account
import com.project.luminacn.constructors.AccountManager
import com.project.luminacn.databinding.FragmentDashboardBinding
import com.project.luminacn.databinding.ItemAccountBinding
import com.project.luminacn.overlay.manager.ConnectionInfoOverlay
import com.project.luminacn.router.main.HomeScreen
import com.project.luminacn.util.InjectNeko
import com.project.luminacn.util.MCPackUtils
import com.project.luminacn.util.ServerInit
import com.project.luminacn.viewmodel.MainScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        
        // 设置账号列表
        setupAccountList()
        
        // 设置按钮监听
        setupButtonListeners()
        
        // 观察ViewModel变化
        observeViewModel()
        
        // 设置Compose部分
        setupComposeView()
    }

    private fun setupComposeView() {
        binding.composeContainer.setContent {
            // 保留完整的HomeScreen逻辑
            HomeScreen(
                onStartToggle = {
                    if (Services.isActive) {
                        Services.stop()
                        updateButtonVisibility()
                    } else {
                        Services.start()
                        updateButtonVisibility()
                        
                        // 启动Minecraft逻辑
                        if (!isLaunchingMinecraft) {
                            isLaunchingMinecraft = true
                            launchMinecraft()
                        }
                    }
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
                
                // 启动Minecraft逻辑
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
        } else if (AccountManager.accounts.value?.isNotEmpty() == true) {
            binding.accountInfoContainer.visibility = View.GONE
            binding.accountList.visibility = View.VISIBLE
        } else {
            binding.accountInfoContainer.visibility = View.GONE
            binding.accountList.visibility = View.GONE
        }
    }

    private fun launchMinecraft() {
        val sharedPreferences = requireContext().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
        val injectNekoPack = sharedPreferences.getBoolean("injectNekoPackEnabled", false)
        
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
                    
                    // 延迟显示连接信息
                    delay(3000)
                    if (Services.isActive) {
                        val disableConnectionInfoOverlay = sharedPreferences.getBoolean("disableConnectionInfoOverlay", false)
                        if (!disableConnectionInfoOverlay) {
                            val localIp = ConnectionInfoOverlay.getLocalIpAddress(requireContext())
                            ConnectionInfoOverlay.show(localIp)
                        }
                    }
                    
                    isLaunchingMinecraft = false
                    
                    // 注入逻辑
                    try {
                        when {
                            injectNekoPack == true && PackSelectionManager.selectedPack != null -> {
                                PackSelectionManager.selectedPack?.let { selectedPack ->
                                    val progressDialog = ProgressDialogFragment().apply {
                                        setCurrentPackName(selectedPack.name)
                                    }
                                    progressDialog.show(parentFragmentManager, "ProgressDialog")
                                    
                                    MCPackUtils.downloadAndOpenPack(requireContext(), selectedPack) { progress ->
                                        progressDialog.updateProgress(progress)
                                    }
                                    
                                    // 下载完成后关闭对话框
                                    progressDialog.dismiss()
                                }
                            }
                            injectNekoPack == true -> {
                                InjectNeko.injectNeko(requireContext()) { progress ->
                                    // 可以在这里更新进度
                                }
                            }
                            else -> {
                                if (selectedGame == "com.mojang.minecraftpe") {
                                    val localIp = ConnectionInfoOverlay.getLocalIpAddress(requireContext())
                                    ServerInit.addMinecraftServer(requireContext(), localIp)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // 显示错误通知
                        withContext(Dispatchers.Main) {
                            SimpleOverlayNotification.show(
                                "错误: ${e.message}",
                                NotificationType.ERROR,
                                5000
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        SimpleOverlayNotification.show(
                            "游戏启动失败，请检查是否安装 Minecraft",
                            NotificationType.ERROR,
                            5000
                        )
                    }
                    isLaunchingMinecraft = false
                }
            } else {
                isLaunchingMinecraft = false
            }
        }
    }

    // 账号适配器
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

    override fun onDestroyView() {
        super.onDestroyView()
        // 清理资源
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
        updateUI()
    }
    
    private fun updateUI() {
        view?.findViewById<TextView>(R.id.downloadText)?.text = "正在下载: $currentPackName"
        view?.findViewById<ProgressBar>(R.id.progressBar)?.progress = (progress * 100).toInt()
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
        updateUI()
    }
}
