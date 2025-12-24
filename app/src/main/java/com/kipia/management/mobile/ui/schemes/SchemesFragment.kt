package com.kipia.management.mobile.ui.schemes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kipia.management.mobile.R
import com.kipia.management.mobile.adapters.SchemeAdapter
import com.kipia.management.mobile.databinding.FragmentSchemesBinding
import com.kipia.management.mobile.viewmodel.SchemesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SchemesFragment : Fragment() {

    private var _binding: FragmentSchemesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SchemesViewModel by viewModels()
    private lateinit var schemeAdapter: SchemeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchemesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        schemeAdapter = SchemeAdapter(
            onItemClick = { scheme ->
                // Открываем редактор схемы
                openSchemeEditor(scheme.id)
            },
            onItemLongClick = { scheme ->
                // Показываем контекстное меню
                showSchemeContextMenu(scheme)
                true
            }
        )

        binding.schemesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = schemeAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allSchemes.observe(viewLifecycleOwner) { schemes ->
            schemeAdapter.submitList(schemes)

            // Показываем/скрываем состояние "нет данных"
            val isEmpty = schemes.isEmpty()
            binding.emptyStateLayout.isVisible = isEmpty
            binding.schemesRecyclerView.isVisible = !isEmpty
        }
    }

    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            createNewScheme()
        }
    }

    private fun openSchemeEditor(schemeId: Int) {
        val action = SchemesFragmentDirections.actionSchemesFragmentToSchemeEditorFragment(
            schemeId = schemeId
        )
        findNavController().navigate(action)
    }

    private fun createNewScheme() {
        // Сразу открываем редактор для новой схемы
        val action = SchemesFragmentDirections.actionSchemesFragmentToSchemeEditorFragment(
            schemeId = 0  // 0 означает "новая схема"
        )
        findNavController().navigate(action)
    }

    private fun showSchemeContextMenu(scheme: com.kipia.management.mobile.data.entities.Scheme) {
        val options = arrayOf(
            getString(R.string.edit),
            getString(R.string.duplicate),
            getString(R.string.delete)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(scheme.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openSchemeEditor(scheme.id) // Редактировать
                    1 -> duplicateScheme(scheme)     // Дублировать
                    2 -> deleteScheme(scheme)        // Удалить
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun duplicateScheme(scheme: com.kipia.management.mobile.data.entities.Scheme) {
        // Создаем копию схемы с приставкой "Копия"
        val copiedScheme = scheme.copy(
            id = 0, // Новый ID будет сгенерирован базой
            name = "${scheme.name} (Копия)",
            description = scheme.description?.let { "$it (Копия)" }
        )

        viewModel.addScheme(copiedScheme)

        // TODO: Также нужно скопировать расположения приборов
        // (пока пропускаем, можно добавить позже)
    }

    private fun deleteScheme(scheme: com.kipia.management.mobile.data.entities.Scheme) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_scheme))
            .setMessage(getString(R.string.delete_scheme_confirmation, scheme.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteScheme(scheme)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}