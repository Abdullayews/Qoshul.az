@file:OptIn(ExperimentalSharedTransitionApi::class)

/*
 * FloatingTabBar v1.0.1 by Elyes Mansour
 * https://github.com/elyesmansour/compose-floating-tab-bar
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Vendored from the v1.0.1 tag: the published AAR is compiled against
 * Compose 1.8 and crashes with NoSuchMethodError on
 * SharedTransitionScope.sharedElement under the Compose version this app uses.
 * Compiling the source here keeps it binary-compatible.
 *
 * Local modifications on top of v1.0.1:
 * - Package renamed.
 * - InlineBar/ExpandedBar use Row/Column instead of ConstraintLayout, and the
 *   tab pill + standalone tab cluster is centered (iOS 26 style) instead of
 *   being pinned to opposite screen edges.
 * - The inline/expanded AnimatedContent is center-aligned so the collapse
 *   animation morphs around the center instead of the start edge.
 *
 * Qoshul.az modifications (D-105, 2026-11-09 — istifadəçi direktivi: iOS 26
 * üslublu üzən naviqasiya; yığılmış halda sol=Kəşf, sağ=Söhbətlər):
 * - getInlineTab HƏMİŞƏ ilk tabı qaytarır (upstream: seçilmiş tabın
 *   yaddaşı) — yığılmış sol həb sabit "Kəşf" olur, istənilən seçimdən asılı olmayaraq.
 * - ExpandedBar enini doldurur (fillMaxWidth): tab həbi weight(1f) ilə
 *   genişlənir və hər slot bərabər pay alır — dar telefonlarda (320-360dp)
 *   mətn kəsilmir; standalone dairə sağda qalır.
 * - animateEnterExitTab: renderEffect yalnız keçid zamanı (blur > 0) qurulur,
 *   istirahət halında null — Robolectric RNG təhlükəsizliyi + perf.
 *
 * Qoshul.az modifications (D-106, 2026-11-09 — istifadəçi direktivi: „açıq
 * halda hamsı eyni pill içində olmalıdır, bağlandıqda isə home və mesajlar
 * öz yerlərində, ortada yox“):
 * - InlineBar (aksesuarsız): klaster mərkəzdə DEYİL — home həbi SOL kənara,
 *   standalone dairə SAĞ kənara sabitlənir (Arrangement.SpaceBetween —
 *   upstream „pinned to opposite edges“ davranışına qayıdış).
 * - ExpandedBar: standalone dairə ayrıca üzgü element DEYİL — HƏBİN
 *   DAXİLİNDƏ, slotların sağında (vahid pill; ExpandedTabs Row-unun
 *   sonuncu uşağı kimi).
 *
 * Qoshul.az modifications (D-111, 2026-11-09 — istifadəçi direktivi:
 * „navigation bar adi halda ana menyu, xəritə, yarat, profil və chat
 * düyməsi yan yana olsun, chat ayrıca yox; aşağı sürüşdürmə
 * animasiyasında həm ana menyu həm də mesajlar dairəviləşib sağa və
 * sola sürüşsün, digərləri isə aşağıya doğru gedib yox olsun“):
 * - AÇIQ HAL: chat artıq standalone DAİRƏ deyil — adi 5-ci slotdur
 *   (etiketli, weight(1f) — AppShell həm tab() həm standaloneTab()
 *   qeydiyyat edir: tab açıq halın slotu, standalone yığılmış halın
 *   sağ dairəsidir). ExpandedTabs-dəki in-pill standalone render LƏĞV.
 * - Chat slotunun İKONU „standaloneTab“ sharedElement-i daşıyır —
 *   yığılmada sağ dairəyə morf edir (birinci tab „tabGroup“+ikon morfu
 *   ilə sola dairəyə keçir — əvvəlki kimi).
 * - animateEnterExitTab: yığılmada qeyri-anker slotlar (xəritə, yarat,
 *   profil + bütün etiketlər) AŞAĞI sürüşüb fade+blur ilə yox olur
 *   (əvvəl yalnız alpha+blur idi — hərəkət əvəzinə AŞAĞI keçid direktivi).
 */

package az.qoshul.app.core.ui.floatingtabbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A floating tab bar that can transition between inline and expanded states.
 *
 * @param isInline controls the FloatingTabBar's inline state
 * @param selectedTabKey the key of the currently selected tab
 * @param modifier the modifier to be applied to the tab bar
 * @param colors the colors used by the tab bar components
 * @param shapes the shapes used by the tab bar components
 * @param sizes the sizes and spacing used by the tab bar components
 * @param elevations the elevation values used by the tab bar components
 * @param tabBarContentModifier modifier applied to the tab bar sections containing the grouped tabs
 *   and standalone tab. It is applied after the default styling (background, shadow, clip) but
 *   before any content padding.
 * @param inlineAccessory the accessory composable that appears in inline state (e.g., compact media
 *   player)
 * @param expandedAccessory the accessory composable that appears in expanded state (e.g., full
 *   media player)
 * @param contentKey optional key that when changed retriggers the content lambda
 * @param content the content defining the tabs
 */
@Composable
fun FloatingTabBar(
  isInline: Boolean,
  selectedTabKey: Any?,
  modifier: Modifier = Modifier,
  tabBarContentModifier: Modifier = Modifier,
  inlineAccessory:
    (@Composable
    SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
    null,
  expandedAccessory:
    (@Composable
    SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
    null,
  colors: FloatingTabBarColors = FloatingTabBarDefaults.colors(),
  shapes: FloatingTabBarShapes = FloatingTabBarDefaults.shapes(),
  sizes: FloatingTabBarSizes = FloatingTabBarDefaults.sizes(),
  elevations: FloatingTabBarElevations = FloatingTabBarDefaults.elevations(),
  contentKey: Any? = null,
  content: FloatingTabBarScope.() -> Unit
) {
  val scrollConnection =
    rememberFloatingTabBarScrollConnection(
      initialIsInline = isInline,
      inlineBehavior = FloatingTabBarInlineBehavior.Never
    )

  LaunchedEffect(isInline) {
    if (isInline) scrollConnection.inline() else scrollConnection.expand()
  }

  FloatingTabBar(
    selectedTabKey = selectedTabKey,
    scrollConnection = scrollConnection,
    modifier = modifier,
    tabBarContentModifier = tabBarContentModifier,
    inlineAccessory = inlineAccessory,
    expandedAccessory = expandedAccessory,
    colors = colors,
    shapes = shapes,
    sizes = sizes,
    elevations = elevations,
    contentKey = contentKey,
    content = content
  )
}

/**
 * A floating tab bar that transitions between inline and expanded states based on scroll behavior.
 *
 * @param selectedTabKey the key of the currently selected tab
 * @param scrollConnection the scroll connection that handles state transitions
 * @param modifier the modifier to be applied to the tab bar
 * @param colors the colors used by the tab bar components
 * @param shapes the shapes used by the tab bar components
 * @param sizes the sizes and spacing used by the tab bar components
 * @param elevations the elevation values used by the tab bar components
 * @param tabBarContentModifier modifier applied to the tab bar sections containing the grouped tabs
 *   and standalone tab. It is applied after the default styling (background, shadow, clip) but
 *   before any content padding.
 * @param inlineAccessory the accessory composable that appears in inline state (e.g., compact media
 *   player)
 * @param expandedAccessory the accessory composable that appears in expanded state (e.g., full
 *   media player)
 * @param contentKey optional key that when changed retriggers the content lambda
 * @param content the content defining the tabs
 */
@Composable
fun FloatingTabBar(
  selectedTabKey: Any?,
  scrollConnection: FloatingTabBarScrollConnection,
  modifier: Modifier = Modifier,
  tabBarContentModifier: Modifier = Modifier,
  inlineAccessory:
    (@Composable
    SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
    null,
  expandedAccessory:
    (@Composable
    SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
    null,
  colors: FloatingTabBarColors = FloatingTabBarDefaults.colors(),
  shapes: FloatingTabBarShapes = FloatingTabBarDefaults.shapes(),
  sizes: FloatingTabBarSizes = FloatingTabBarDefaults.sizes(),
  elevations: FloatingTabBarElevations = FloatingTabBarDefaults.elevations(),
  contentKey: Any? = null,
  content: FloatingTabBarScope.() -> Unit
) {
  val scope = remember(contentKey) { FloatingTabBarScopeImpl().apply { content() } }

  val isAccessoryShared = inlineAccessory != null && expandedAccessory != null

  SharedTransitionLayout(modifier = modifier) {
    AnimatedContent(
      targetState = scrollConnection.isInline,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      contentAlignment = Alignment.BottomCenter
    ) { isInline ->
      if (isInline) {
        InlineBar(
          scope = scope,
          selectedTabKey = selectedTabKey,
          accessory = inlineAccessory,
          isAccessoryShared = isAccessoryShared,
          onInlineTabClick = { scrollConnection.expand() },
          colors = colors,
          shapes = shapes,
          sizes = sizes,
          elevations = elevations,
          tabBarContentModifier = tabBarContentModifier,
          animatedVisibilityScope = this@AnimatedContent
        )
      } else {
        ExpandedBar(
          scope = scope,
          selectedTabKey = selectedTabKey,
          accessory = expandedAccessory,
          isAccessoryShared = isAccessoryShared,
          colors = colors,
          shapes = shapes,
          sizes = sizes,
          elevations = elevations,
          tabBarContentModifier = tabBarContentModifier,
          animatedVisibilityScope = this@AnimatedContent
        )
      }
    }
  }
}

/**
 * A [NestedScrollConnection] that handles scroll events to transition between inline and expanded
 * states.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded).
 * @param scrollThresholdPx The minimum scroll distance in pixels required to trigger a state
 *   change.
 * @param inlineBehavior Defines when the tab bar should transition to inline state.
 */
class FloatingTabBarScrollConnection(
  initialIsInline: Boolean = false,
  private val scrollThresholdPx: Float,
  private val inlineBehavior: FloatingTabBarInlineBehavior =
    FloatingTabBarInlineBehavior.OnScrollDown
) : NestedScrollConnection {
  var isInline by mutableStateOf(initialIsInline)
    private set

  private var accumulatedScroll = 0f

  fun expand() {
    isInline = false
    accumulatedScroll = 0f
  }

  fun inline() {
    isInline = true
    accumulatedScroll = 0f
  }

  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    // If behavior is Never, don't change state
    if (inlineBehavior == FloatingTabBarInlineBehavior.Never) {
      return Offset.Zero
    }

    val scrollDelta = available.y

    // Reset accumulated scroll if changing direction
    if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
      accumulatedScroll = 0f
    }

    // Accumulate scroll
    accumulatedScroll += scrollDelta

    when (inlineBehavior) {
      FloatingTabBarInlineBehavior.OnScrollDown -> {
        // Check if we've scrolled enough to trigger state change
        if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
          // Scrolling down enough - transition to inline mode
          isInline = true
          accumulatedScroll = 0f // Reset after state change
        } else if (accumulatedScroll >= scrollThresholdPx && isInline) {
          // Scrolling up enough - transition to expanded mode
          isInline = false
          accumulatedScroll = 0f // Reset after state change
        }
      }
      FloatingTabBarInlineBehavior.OnScrollUp -> {
        // Check if we've scrolled enough to trigger state change
        if (accumulatedScroll >= scrollThresholdPx && !isInline) {
          // Scrolling up enough - transition to inline mode
          isInline = true
          accumulatedScroll = 0f // Reset after state change
        } else if (accumulatedScroll <= -scrollThresholdPx && isInline) {
          // Scrolling down enough - transition to expanded mode
          isInline = false
          accumulatedScroll = 0f // Reset after state change
        }
      }
      FloatingTabBarInlineBehavior.Never -> {
        // Already handled above, but included for completeness
      }
    }

    return Offset.Zero // Don't consume the scroll, let it pass through
  }
}

/**
 * Creates and remembers a [FloatingTabBarScrollConnection] instance.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded). Default is false.
 * @param scrollThreshold The minimum scroll distance required to trigger a state change. Default is
 *   50.dp.
 * @param inlineBehavior Defines when the tab bar should transition to inline state. Default is
 *   [FloatingTabBarInlineBehavior.OnScrollDown].
 * @return A remembered [FloatingTabBarScrollConnection] instance.
 */
@Composable
fun rememberFloatingTabBarScrollConnection(
  initialIsInline: Boolean = false,
  scrollThreshold: Dp = 50.dp,
  inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
): FloatingTabBarScrollConnection =
  with(LocalDensity.current) {
    val scrollThresholdPx = scrollThreshold.toPx()
    remember(scrollThresholdPx, inlineBehavior, initialIsInline) {
      FloatingTabBarScrollConnection(initialIsInline, scrollThresholdPx, inlineBehavior)
    }
  }

/** Defines when the floating tab bar should transition to inline state. */
enum class FloatingTabBarInlineBehavior {
  /** Never transition to inline - it stays in expanded state */
  Never,

  /** Transition to inline when scrolling down */
  OnScrollDown,

  /** Transition to inline when scrolling up */
  OnScrollUp
}

interface FloatingTabBarScope {
  /**
   * Adds a regular tab to the floating tab bar.
   *
   * @param key Unique identifier for the tab
   * @param title Composable content for the tab title
   * @param icon Composable content for the tab icon
   * @param onClick Callback invoked when the tab is clicked
   * @param indication Optional indication provider for touch feedback, defaults to
   *   LocalIndication.current
   */
  fun tab(
    key: Any,
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    indication: (@Composable () -> Indication)? = { LocalIndication.current }
  )

  /**
   * Adds a standalone tab to the floating tab bar.
   *
   * Note: Calling this method more than once will override the previous standalone tab value.
   *
   * @param key Unique identifier for the standalone tab
   * @param icon Composable content for the tab icon
   * @param onClick Callback invoked when the tab is clicked
   * @param indication Optional indication provider for touch feedback, defaults to
   *   LocalIndication.current
   */
  fun standaloneTab(
    key: Any,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    indication: (@Composable () -> Indication)? = { LocalIndication.current }
  )
}

@Composable
private fun SharedTransitionScope.InlineBar(
  scope: FloatingTabBarScopeImpl,
  selectedTabKey: Any?,
  accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
  isAccessoryShared: Boolean,
  onInlineTabClick: () -> Unit,
  colors: FloatingTabBarColors,
  shapes: FloatingTabBarShapes,
  sizes: FloatingTabBarSizes,
  elevations: FloatingTabBarElevations,
  tabBarContentModifier: Modifier,
  animatedVisibilityScope: AnimatedVisibilityScope
) {
  val inlineTab = scope.getInlineTab(selectedTabKey)
  val standaloneTab = scope.standaloneTab
  val hasInlineTab = inlineTab != null

  // With an accessory the row spans the full width ([tab][accessory][standalone],
  // iOS 26 Apple Music style); without one, the home pill pins to the LEFT edge
  // and the standalone tab to the RIGHT edge (D-106: "own places, not in the
  // middle" — upstream pinned-edges behavior restored).
  Row(
    horizontalArrangement =
      if (accessory == null) Arrangement.SpaceBetween
      else Arrangement.spacedBy(sizes.componentSpacing),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth().height(56.dp)
  ) {
    if (hasInlineTab) {
      InlineTab(
        inlineTab = inlineTab,
        onInlineTabClick = onInlineTabClick,
        shapes = shapes,
        sizes = sizes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        tabBarContentModifier = tabBarContentModifier,
        modifier = Modifier.fillMaxHeight().aspectRatio(1f)
      )
    }

    if (accessory != null) {
      InlineAccessory(
        accessory = accessory,
        isAccessoryShared = isAccessoryShared,
        shapes = shapes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
    }

    if (standaloneTab != null) {
      InlineStandaloneTab(
        standaloneTab = standaloneTab,
        shapes = shapes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        tabBarContentModifier = tabBarContentModifier,
        modifier = Modifier.fillMaxHeight().aspectRatio(1f)
      )
    }
  }
}

@Composable
private fun SharedTransitionScope.InlineTab(
  inlineTab: FloatingTabBarTab,
  onInlineTabClick: () -> Unit,
  shapes: FloatingTabBarShapes,
  sizes: FloatingTabBarSizes,
  colors: FloatingTabBarColors,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier,
  tabBarContentModifier: Modifier
) {
  val pressSource = remember { MutableInteractionSource() }
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .pressScale(pressSource)
        .sharedElement(
          sharedContentState = rememberSharedContentState("tabGroup"),
          animatedVisibilityScope = animatedVisibilityScope,
          zIndexInOverlay = 1f
        )
        .shadow(shape = shapes.tabBarShape, elevation = elevations.inlineElevation)
        .background(color = colors.backgroundColor, shape = shapes.tabBarShape)
        .clip(shapes.tabBarShape)
        .then(tabBarContentModifier)
        .combinedClickable(
          onClick = {
            onInlineTabClick()
            inlineTab.onClick()
          },
          onLongClick = inlineTab.onLongClick,
          role = Role.Tab,
          indication = inlineTab.indication?.invoke(),
          interactionSource = pressSource
        )
        .padding(sizes.tabInlineContentPadding)
  ) {
    Tab(
      icon = {
        Box(
          Modifier.sharedElement(
            sharedContentState = rememberSharedContentState("tab#${inlineTab.key}-icon"),
            animatedVisibilityScope = animatedVisibilityScope,
            zIndexInOverlay = 1f
          )
        ) {
          inlineTab.icon()
        }
      },
      title = { inlineTab.title() },
      isInline = true
    )
  }
}

@Composable
private fun SharedTransitionScope.InlineStandaloneTab(
  standaloneTab: FloatingTabBarTab,
  shapes: FloatingTabBarShapes,
  colors: FloatingTabBarColors,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier,
  tabBarContentModifier: Modifier
) {
  val pressSource = remember { MutableInteractionSource() }
  Tab(
    icon = standaloneTab.icon,
    title = standaloneTab.title,
    isInline = true,
    isStandalone = true,
    modifier =
      modifier
        .pressScale(pressSource)
        .sharedElement(
          sharedContentState = rememberSharedContentState("standaloneTab"),
          animatedVisibilityScope = animatedVisibilityScope,
          zIndexInOverlay = 1f
        )
        .shadow(shape = shapes.standaloneTabShape, elevation = elevations.inlineElevation)
        .background(color = colors.backgroundColor, shape = shapes.standaloneTabShape)
        .clip(shapes.standaloneTabShape)
        .then(tabBarContentModifier)
        .combinedClickable(
          onClick = standaloneTab.onClick,
          onLongClick = standaloneTab.onLongClick,
          role = Role.Tab,
          indication = standaloneTab.indication?.invoke(),
          interactionSource = pressSource
        )
  )
}

@Composable
private fun SharedTransitionScope.InlineAccessory(
  accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
  isAccessoryShared: Boolean,
  colors: FloatingTabBarColors,
  shapes: FloatingTabBarShapes,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier
) {
  accessory?.let { accessory ->
    Box(
      modifier =
        modifier.then(
          if (isAccessoryShared) {
            Modifier.sharedElement(
              sharedContentState = rememberSharedContentState("accessory"),
              animatedVisibilityScope = animatedVisibilityScope
            )
          } else {
            Modifier.animateEnterExitAccessory(
              sharedTransitionScope = this,
              animatedVisibilityScope = animatedVisibilityScope
            )
          }
        )
    ) {
      accessory(
        Modifier.fillMaxSize()
          .shadow(shape = shapes.accessoryShape, elevation = elevations.inlineElevation)
          .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
          .clip(shapes.accessoryShape),
        animatedVisibilityScope
      )
    }
  }
}

@Composable
private fun SharedTransitionScope.ExpandedBar(
  scope: FloatingTabBarScopeImpl,
  selectedTabKey: Any?,
  accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
  isAccessoryShared: Boolean,
  colors: FloatingTabBarColors,
  shapes: FloatingTabBarShapes,
  sizes: FloatingTabBarSizes,
  elevations: FloatingTabBarElevations,
  tabBarContentModifier: Modifier,
  animatedVisibilityScope: AnimatedVisibilityScope
) {
  val standaloneTab = scope.standaloneTab
  val hasTabGroup = scope.tabs.isNotEmpty()

  // The accessory spans the full width above the tab row. Qoshul (D-105):
  // aksesuar olmadığı halda da sütun eni doldurur — tab həbi weight ilə
  // genişlənir, dar ekranda mətn kəsilmir. Qoshul (D-106, istifadəçi
  // direktivi: „açıq halda hamsı eyni pill içində olmalıdır“): standalone
  // dairə artıq ayrı üzgü element DEYİL — ExpandedTabs həbinin DAXİLİNDƏ
  // render olunur (vahid pill). Yalnız tablar ümumiyyətlə yoxdursa upstream
  // fallback: standalone öz dairəsində qalır.
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
    modifier =
      if (accessory != null) Modifier.widthIn(max = 480.dp).fillMaxWidth()
      else Modifier.fillMaxWidth()
  ) {
    if (accessory != null) {
      ExpandedAccessory(
        accessory = accessory,
        isAccessoryShared = isAccessoryShared,
        shapes = shapes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier.fillMaxWidth().height(56.dp)
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(sizes.componentSpacing),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
      if (hasTabGroup) {
        ExpandedTabs(
          scope = scope,
          selectedTabKey = selectedTabKey,
          shapes = shapes,
          sizes = sizes,
          colors = colors,
          elevations = elevations,
          animatedVisibilityScope = animatedVisibilityScope,
          tabBarContentModifier = tabBarContentModifier,
          // Qoshul (D-105): həb qalan eni alır (D-106: dairə artıq daxildə)
          modifier = Modifier.weight(1f).fillMaxHeight()
        )
      } else if (standaloneTab != null) {
        // upstream fallback — tab qrupu yoxdursa dairə tək üzgü elementdir
        ExpandedStandaloneTab(
          standaloneTab = standaloneTab,
          shapes = shapes,
          colors = colors,
          elevations = elevations,
          animatedVisibilityScope = animatedVisibilityScope,
          tabBarContentModifier = tabBarContentModifier,
          modifier = Modifier.fillMaxHeight().aspectRatio(1f)
        )
      }
    }
  }
}

@Composable
private fun SharedTransitionScope.ExpandedAccessory(
  accessory: @Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit,
  isAccessoryShared: Boolean,
  colors: FloatingTabBarColors,
  shapes: FloatingTabBarShapes,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier
) {
  Box(
    modifier =
      modifier.then(
        if (isAccessoryShared) {
          Modifier.sharedElement(
            sharedContentState = rememberSharedContentState("accessory"),
            animatedVisibilityScope = animatedVisibilityScope
          )
        } else {
          Modifier.animateEnterExitAccessory(
            sharedTransitionScope = this,
            animatedVisibilityScope = animatedVisibilityScope
          )
        }
      )
  ) {
    accessory(
      Modifier.shadow(shape = shapes.accessoryShape, elevation = elevations.expandedElevation)
        .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
        .clip(shapes.accessoryShape),
      animatedVisibilityScope
    )
  }
}

@Composable
private fun SharedTransitionScope.ExpandedTabs(
  scope: FloatingTabBarScopeImpl,
  selectedTabKey: Any?,
  shapes: FloatingTabBarShapes,
  sizes: FloatingTabBarSizes,
  colors: FloatingTabBarColors,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier,
  tabBarContentModifier: Modifier
) {
  val inlineTab = scope.getInlineTab(selectedTabKey)
  val tabWidths = remember { mutableStateMapOf<Int, Dp>() }
  val tabHeights = remember { mutableStateMapOf<Int, Dp>() }
  val tabOffsets = remember { mutableStateMapOf<Int, Dp>() }
  val density = LocalDensity.current

  val selectedIndex = scope.tabs.indexOfFirst { it.key == selectedTabKey }.takeIf { it >= 0 } ?: 0
  val targetWidth = tabWidths[selectedIndex] ?: 0.dp
  val targetHeight = tabHeights[selectedIndex] ?: 0.dp
  val targetOffset = tabOffsets[selectedIndex] ?: 0.dp

  val animatedWidth by
    animateDpAsState(
      targetValue = targetWidth,
      label = "width",
      animationSpec = spring(stiffness = 380f, dampingRatio = 0.72f)
    )
  val animatedHeight by
    animateDpAsState(
      targetValue = targetHeight,
      label = "height",
      animationSpec = spring(stiffness = 380f, dampingRatio = 0.72f)
    )
  val animatedOffset by
    animateDpAsState(
      targetValue = targetOffset,
      label = "offset",
      animationSpec = spring(stiffness = 380f, dampingRatio = 0.72f)
    )

  Box(
    contentAlignment = Alignment.CenterStart,
    modifier =
      modifier
        .sharedElement(
          sharedContentState = rememberSharedContentState("tabGroup"),
          animatedVisibilityScope = animatedVisibilityScope,
          zIndexInOverlay = 1f
        )
        .shadow(shape = shapes.tabBarShape, elevation = elevations.expandedElevation)
        .background(color = colors.backgroundColor, shape = shapes.tabBarShape)
        .clip(shapes.tabBarShape)
        .then(tabBarContentModifier)
        .padding(sizes.tabBarContentPadding)
        .animateContentSize()
  ) {
    if (targetWidth > 0.dp) {
      Box(
        Modifier.offset(x = animatedOffset)
          .width(animatedWidth)
          .height(animatedHeight)
          .clip(shapes.tabShape)
          .background(colors.selectedIndicatorColor)
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(sizes.tabSpacing, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      scope.tabs.forEachIndexed { index, tab ->
        val pressSource = remember { MutableInteractionSource() }
        Tab(
          isSelected = (tab.key == selectedTabKey),
          icon = {
            Box(
              modifier =
                if (tab.key == inlineTab?.key) {
                  Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState("tab#${tab.key}-icon"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    zIndexInOverlay = 1f
                  )
                } else if (tab.key == scope.standaloneTab?.key) {
                  // Qoshul (D-111): chat slotu — yığılmış sağ dairə ilə morf
                  // („standaloneTab“ sharedElement-i: icon-box ↔ dairə)
                  Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState("standaloneTab"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    zIndexInOverlay = 1f
                  )
                } else {
                  Modifier.animateEnterExitTab(
                    sharedTransitionScope = this@ExpandedTabs,
                    animatedVisibilityScope = animatedVisibilityScope
                  )
                }
            ) {
              tab.icon()
            }
          },
          title = {
            Box(
              Modifier.animateEnterExitTab(
                sharedTransitionScope = this@ExpandedTabs,
                animatedVisibilityScope = animatedVisibilityScope
              )
            ) {
              tab.title()
            }
          },
          isInline = false,
          modifier =
            Modifier.onGloballyPositioned { coords ->
                tabWidths[index] = with(density) { coords.size.width.toDp() }
                tabHeights[index] = with(density) { coords.size.height.toDp() }
                tabOffsets[index] = with(density) { coords.positionInParent().x.toDp() }
              }
              // Qoshul (D-105): bərabər slot paylaşımı — dar ekranda da sığır.
              // QEYD: upstream skipToLookaheadSize() burada ÇIXARILIB — weight
              // paylanması lookahead ölçüsü ilə ziddiyyətə düşürdü (tablar təbii
              // endə qalırdı); indikator onGloballyPositioned-dən ölçülür.
              .weight(1f)
              .clip(shapes.tabShape)
              .combinedClickable(
                onClick = tab.onClick,
                onLongClick = tab.onLongClick,
                role = Role.Tab,
                indication = tab.indication?.invoke(),
                interactionSource = pressSource
              )
              .padding(sizes.tabExpandedContentPadding)
              .pressScale(pressSource, pressedScale = 0.9f)
        )
      }

      // Qoshul (D-111): in-pill standalone render LƏĞV — chat (standalone
      // qeydiyyatlı tab) artıq yuxarıdakı Row-da ADİ 5-ci slotdur; upstream
      // ExpandedStandaloneTab yalnız „tablar ümumiyyətlə yoxdur“ fallback-ində
      // qalır (ExpandedBar-ın else-budağı).
    }
  }
}

@Composable
private fun SharedTransitionScope.ExpandedStandaloneTab(
  standaloneTab: FloatingTabBarTab,
  shapes: FloatingTabBarShapes,
  colors: FloatingTabBarColors,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier,
  tabBarContentModifier: Modifier
) {
  val pressSource = remember { MutableInteractionSource() }
  Tab(
    icon = standaloneTab.icon,
    title = standaloneTab.title,
    isInline = false,
    isStandalone = true,
    modifier =
      modifier
        .pressScale(pressSource)
        .sharedElement(
          sharedContentState = rememberSharedContentState("standaloneTab"),
          animatedVisibilityScope = animatedVisibilityScope,
          zIndexInOverlay = 1f
        )
        .shadow(shape = shapes.standaloneTabShape, elevation = elevations.expandedElevation)
        .background(color = colors.backgroundColor, shape = shapes.standaloneTabShape)
        .clip(shapes.standaloneTabShape)
        .then(tabBarContentModifier)
        .combinedClickable(
          onClick = standaloneTab.onClick,
          onLongClick = standaloneTab.onLongClick,
          role = Role.Tab,
          indication = standaloneTab.indication?.invoke(),
          interactionSource = pressSource
        )
  )
}

/** iOS style press feedback: squeezes slightly while pressed and springs back on release. */
@Composable
private fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.94f): Modifier {
  val pressed by source.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) pressedScale else 1f,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
      label = "pressScale"
    )
  return graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}

@Composable
private fun Tab(
  icon: @Composable () -> Unit,
  title: @Composable () -> Unit,
  isInline: Boolean,
  modifier: Modifier = Modifier,
  isStandalone: Boolean = false,
  isSelected: Boolean = false
) {
  if (!isStandalone && !isInline) {
    // iOS style: icon on top, label underneath, label always visible.
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = modifier
    ) {
      icon()
      Box(Modifier.padding(top = 2.dp)) { title() }
    }
  } else {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = modifier
    ) {
      icon()
    }
  }
}

/**
 * A custom modifier that provides smooth enter/exit animations without clipping shadows or other
 * content. This is an alternative to animateEnterExit that uses
 * renderInSharedTransitionScopeOverlay to prevent clipping.
 */
@Composable
private fun Modifier.animateEnterExitAccessory(
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope
): Modifier =
  with(sharedTransitionScope) {
    with(animatedVisibilityScope) {
      val animatedAlpha by
        transition.animateFloat { targetState ->
          when (targetState) {
            EnterExitState.Visible -> 1f
            else -> 0f
          }
        }

      this@animateEnterExitAccessory.renderInSharedTransitionScopeOverlay()
        .graphicsLayer(
          compositingStrategy = CompositingStrategy.ModulateAlpha,
          alpha = animatedAlpha
        )
    }
  }

/** A custom modifier that provides smooth enter/exit animations with fade, slide-DOWN and blur effects.
 *  Qoshul (D-111): qeyri-anker slotlar yığılmada AŞAĞI sürüşür (direktiv:
 *  „digərləri isə aşağıya doğru gedib yox olsun“) — 0→26dp translationY
 *  alpha və blur ilə birlikdə. */
@Composable
private fun Modifier.animateEnterExitTab(
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope
): Modifier =
  with(sharedTransitionScope) {
    with(animatedVisibilityScope) {
      val enterStartFraction = 0.5f
      val enterEndFraction = 0.8f
      val durationMs = 150

      val animatedAlpha by
        transition.animateFloat(
          transitionSpec = {
            keyframes {
              durationMillis = durationMs
              if (targetState == EnterExitState.Visible) {
                0f atFraction enterStartFraction using FastOutSlowInEasing
                1f atFraction enterEndFraction
              }
            }
          }
        ) { targetState ->
          when (targetState) {
            EnterExitState.Visible -> 1f
            else -> 0f
          }
        }

      val blurRadius = with(LocalDensity.current) { 50.dp.toPx() }
      // Qoshul (D-111): görünən = 0, çıxan/girən = 1 (aşağı ofset çarpanı)
      val animatedShift by
        transition.animateFloat { targetState ->
          when (targetState) {
            EnterExitState.Visible -> 0f
            else -> 1f
          }
        }
      val animatedBlur by
        transition.animateFloat(
          transitionSpec = {
            keyframes {
              durationMillis = durationMs
              if (targetState == EnterExitState.Visible) {
                blurRadius atFraction enterStartFraction using FastOutSlowInEasing
                0f atFraction enterEndFraction
              }
            }
          }
        ) { targetState ->
          when (targetState) {
            EnterExitState.Visible -> 0f
            else -> blurRadius
          }
        }

      graphicsLayer {
        alpha = animatedAlpha
        translationY = 26.dp.toPx() * animatedShift
        // Qoshul (D-105): renderEffect yalnız keçiddə — istirahətdə null
        // (Robolectric RNG + perf təhlükəsizliyi)
        if (animatedBlur > 0.5f) {
          renderEffect = BlurEffect(radiusX = animatedBlur, radiusY = animatedBlur)
        } else {
          renderEffect = null
        }
      }
    }
  }

private class FloatingTabBarScopeImpl : FloatingTabBarScope {
  val tabs = mutableStateListOf<FloatingTabBarTab>()
  var standaloneTab: FloatingTabBarTab? by mutableStateOf(null)
    private set

  private var inlineTab: FloatingTabBarTab? = null

  fun getInlineTab(selectedTabKey: Any?): FloatingTabBarTab? {
    // Qoshul (D-105): yığılmış sol həb HƏMİŞƏ ilk tab (Kəşf) — istifadəçi
    // direktivi: "yığılmış haldakı sağ və solda olan düymələr home və chat
    // düyməsi olsun". Upstream-in seçilmiş-tab yaddaşı istifadə olunmur.
    return tabs.firstOrNull()
  }

  override fun tab(
    key: Any,
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    indication: (@Composable () -> Indication)?
  ) {
    tabs.add(
      FloatingTabBarTab(
        key = key,
        title = title,
        icon = icon,
        onClick = onClick,
        onLongClick = onLongClick,
        indication = indication
      )
    )
  }

  override fun standaloneTab(
    key: Any,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    indication: (@Composable () -> Indication)?
  ) {
    standaloneTab =
      FloatingTabBarTab(
        key = key,
        title = {},
        icon = icon,
        onClick = onClick,
        onLongClick = onLongClick,
        indication = indication
      )
  }
}

private data class FloatingTabBarTab(
  val key: Any,
  val title: @Composable () -> Unit,
  val icon: @Composable () -> Unit,
  val onClick: () -> Unit,
  val onLongClick: (() -> Unit)? = null,
  val indication: (@Composable () -> Indication)?
)

/** Represents the colors used in [FloatingTabBar]. */
@Immutable
data class FloatingTabBarColors(
  val backgroundColor: Color,
  val accessoryBackgroundColor: Color,
  val selectedIndicatorColor: Color = Color.White.copy(alpha = 0.25f),
)

/** Represents the shapes used in [FloatingTabBar]. */
@Immutable
data class FloatingTabBarShapes(
  val tabBarShape: Shape,
  val tabShape: Shape,
  val standaloneTabShape: Shape,
  val accessoryShape: Shape,
)

/** Represents the elevations used in [FloatingTabBar]. */
@Immutable
data class FloatingTabBarElevations(
  val inlineElevation: Dp,
  val expandedElevation: Dp,
)

/** Represents the sizes and spacing used in [FloatingTabBar]. */
@Immutable
data class FloatingTabBarSizes(
  val tabBarContentPadding: PaddingValues,
  val tabInlineContentPadding: PaddingValues,
  val tabExpandedContentPadding: PaddingValues,
  val componentSpacing: Dp,
  val tabSpacing: Dp,
)

/** Contains the default values used by [FloatingTabBar]. */
object FloatingTabBarDefaults {
  /**
   * Creates a [FloatingTabBarColors] that represents the default colors used in a [FloatingTabBar].
   *
   * @param backgroundColor the color used for the tab bar background
   * @param accessoryBackgroundColor the color used for the accessory background
   */
  @Composable
  fun colors(
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    accessoryBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedIndicatorColor: Color = Color.White.copy(alpha = 0.25f),
  ): FloatingTabBarColors =
    FloatingTabBarColors(
      backgroundColor = backgroundColor,
      accessoryBackgroundColor = accessoryBackgroundColor,
      selectedIndicatorColor = selectedIndicatorColor,
    )

  /**
   * Creates a [FloatingTabBarShapes] that represents the default shapes used in a [FloatingTabBar].
   *
   * @param tabBarShape the shape used to clip the tab bar
   * @param tabShape the shape used to clip individual tabs. Can be useful for example to control
   *   the click ripple effect shape
   * @param standaloneTabShape the shape used to clip the standalone tab
   * @param accessoryShape the shape used to clip the accessory container
   */
  @Composable
  fun shapes(
    tabBarShape: Shape = RoundedCornerShape(100),
    tabShape: Shape = RoundedCornerShape(100),
    standaloneTabShape: Shape = CircleShape,
    accessoryShape: Shape = RoundedCornerShape(100),
  ): FloatingTabBarShapes =
    FloatingTabBarShapes(
      tabBarShape = tabBarShape,
      tabShape = tabShape,
      standaloneTabShape = standaloneTabShape,
      accessoryShape = accessoryShape,
    )

  /**
   * Creates a [FloatingTabBarSizes] that represents the default sizes used in a [FloatingTabBar].
   *
   * @param tabBarContentPadding the padding applied to the tab bar content. This also applies to
   *   the standalone tab content.
   * @param tabInlineContentPadding the padding applied to tabs in inline state
   * @param tabExpandedContentPadding the padding applied to tabs in expanded state
   * @param componentSpacing the spacing between components
   * @param tabSpacing the spacing between tabs in expanded state
   */
  @Composable
  fun sizes(
    tabBarContentPadding: PaddingValues = PaddingValues(vertical = 2.dp, horizontal = 4.dp),
    tabInlineContentPadding: PaddingValues = PaddingValues(10.dp),
    tabExpandedContentPadding: PaddingValues = PaddingValues(vertical = 2.dp, horizontal = 20.dp),
    componentSpacing: Dp = 8.dp,
    tabSpacing: Dp = 0.dp,
  ): FloatingTabBarSizes =
    FloatingTabBarSizes(
      tabBarContentPadding = tabBarContentPadding,
      tabInlineContentPadding = tabInlineContentPadding,
      tabExpandedContentPadding = tabExpandedContentPadding,
      componentSpacing = componentSpacing,
      tabSpacing = tabSpacing,
    )

  /**
   * Creates a [FloatingTabBarElevations] that represents the default elevations used in a
   * [FloatingTabBar].
   *
   * @param inlineElevation the elevation used for tabs in inline state
   * @param expandedElevation the elevation used for tabs in expanded state
   */
  @Composable
  fun elevations(
    inlineElevation: Dp = 6.dp,
    expandedElevation: Dp = 12.dp,
  ): FloatingTabBarElevations =
    FloatingTabBarElevations(
      inlineElevation = inlineElevation,
      expandedElevation = expandedElevation,
    )
}
