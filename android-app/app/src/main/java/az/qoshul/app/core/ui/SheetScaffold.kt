package az.qoshul.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.qoshul.app.core.theme.Q

/**
 * Vahid modal bottom sheet — Flutter showModalBottomSheet portu:
 * card fon, üst radius 8 (Q.rLg), drag handle YOX, tutamaq xətti.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetScaffold(
    onDismiss: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Q.card,
        shape = RoundedCornerShape(topStart = Q.rLg.dp, topEnd = Q.rLg.dp),
        dragHandle = null,
        scrimColor = Q.foreground.copy(alpha = 0.45f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                // D-053 fix: imePadding — mətn sahəli sheet-lərdə (Yarat, Rezerv,
                // rəy) klaviatura məzmunu örtürdü. Inset istehlakı qaydasına görə
                // navigationBarsPadding + imePadding = max(nav, ime) — klaviatura
                // açıqda yalnız ime, qapalıyken yalnız nav tətbiq olunur.
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 12.dp)
        ) {
            // tutamaq xətti (Flutter-dəki 36x4 qapaq)
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 14.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Q.border, RoundedCornerShape(2.dp))
            )
            content()
        }
    }
}
