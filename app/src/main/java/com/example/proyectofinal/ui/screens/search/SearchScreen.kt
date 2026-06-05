package com.example.proyectofinal.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyectofinal.data.repository.GamerRepository
import com.example.proyectofinal.ui.components.FilterBottomSheet
import com.example.proyectofinal.ui.components.GamerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: GamerRepository,
    onNavigate: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory(repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = uiState.query,
                            onQueryChange = { viewModel.onQueryChanged(it) },
                            onSearch = { viewModel.onSearch() },
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Buscar jugadores...") }
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.weight(1f)
                ) {}

                IconButton(onClick = { viewModel.onShowFilters() }) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = "Filtros",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (uiState.query.length in 0..1 && uiState.query.isNotEmpty()) {
                Text(
                    text = "Ingresa al menos 2 caracteres",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (uiState.hasSearched && uiState.filteredGamers.isEmpty()) {
                Text(
                    text = "No se encontraron resultados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredGamers) { gamer ->
                    GamerCard(
                        gamer = gamer,
                        onClick = { onNavigate("profile/${gamer.id}") }
                    )
                }
            }
        }

        if (uiState.showFilters) {
            FilterBottomSheet(
                onDismiss = { viewModel.onDismissFilters() },
                onApply = { level, _, platform ->
                    viewModel.onFilterApplied(level, platform)
                }
            )
        }
    }
}
