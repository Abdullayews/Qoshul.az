package az.qoshul.app.features.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.SheetScaffold

/** "Şəhərini seç" — Flutter city_picker_sheet.dart portu. */
@Composable
fun CityPickerSheet(engine: AppEngine, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val st by engine.state.collectAsState()
    val list = engine.dataSource.cities.filter {
        it.name.lowercase().contains(query.lowercase())
    }

    SheetScaffold(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Şəhərini seç", style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 18.sp, fontWeight = FontWeight.W800))
            Spacer(Modifier.height(4.dp))
            Text("Azərbaycanın ${engine.dataSource.cities.size} şəhər və rayon mərkəzindən birini seç.",
                style = T.subtitle)
            Spacer(Modifier.height(12.dp))
            // axtarış sətri
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(43.dp)
                    .background(Q.card, RoundedCornerShape(7.dp))
                    .androidxSearchBorder()
                    .padding(horizontal = 12.dp),
            ) {
                Icon(Icons.Filled.Search, null, tint = Q.mutedFg, modifier = Modifier.height(16.dp))
                Spacer(Modifier.padding(4.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = T.input,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.height(420.dp)) {
                if (list.isEmpty()) {
                    item { Text("Şəhər tapılmadı.", style = T.subtitle) }
                }
                items(list) { c ->
                    val active = st.city == c.name
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .androidxCityRow(active) {
                                engine.setCity(c.name)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    ) {
                        Text(c.name, style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.W700 else FontWeight.W400,
                            color = if (active) Q.primary else Q.foreground))
                        Text(c.region, style = T.meta)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun Modifier.androidxCityRow(active: Boolean, onClick: () -> Unit): Modifier = this.then(
    Modifier
        .clickable(onClick = onClick)
        .background(if (active) Q.secondary else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp))
)

private fun Modifier.androidxSearchBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(7.dp))
)
