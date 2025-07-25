package com.project.luminacn.phoenix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.project.luminacn.viewmodel.MainScreenViewModel

class DashboardFragment : Fragment() {

    private val viewModel: MainScreenViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            DashboardScreen(viewModel)
        }
    }
}
