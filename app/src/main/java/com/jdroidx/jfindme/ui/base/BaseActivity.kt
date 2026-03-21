package com.jdroidx.jfindme.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * BaseActivity to standardize ViewBinding and common UI logic.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    abstract fun inflateBinding(inflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
    }

    /**
     * Initialize UI components, listeners, etc.
     */
    abstract fun setupUI()

    /**
     * Observe LiveData from ViewModel.
     */
    abstract fun observeViewModel()

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
