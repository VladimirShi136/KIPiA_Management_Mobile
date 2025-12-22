package com.kipia.management.mobile.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kipia.management.mobile.adapters.DeviceAdapter
import com.kipia.management.mobile.databinding.FragmentDevicesBinding
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DevicesViewModel by viewModels()
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device ->
            // Навигация к деталям прибора
            val action = DevicesFragmentDirections.actionDevicesFragmentToDeviceDetailFragment(
                deviceId = device.id
            )
            findNavController().navigate(action)
        }

        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allDevices.observe(viewLifecycleOwner) { devices ->
            deviceAdapter.submitList(devices)
        }
    }

    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            // Навигация к экрану добавления нового прибора
            findNavController().navigate(
                DevicesFragmentDirections.actionDevicesFragmentToDeviceEditFragment()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}