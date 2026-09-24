package com.bakingbuddy.repositories.helpers

import com.bakingbuddy.api.errors.BadRequestException
import com.bakingbuddy.api.errors.NotFoundException
import kotlin.uuid.Uuid

/** Spacing between neighbouring `order` values, leaving room to insert steps without renumbering. */
const val ORDER_GAP = 10

/**
 * Picks an order strictly between two neighbours (either may be absent, meaning "start" / "end").
 * Returns null when there is no integer room left, so the caller knows to rebalance and retry.
 */
fun computeMidpointOrder(
  previousOrder: Int?,
  nextOrder: Int?,
): Int? =
  when {
    previousOrder == null && nextOrder == null -> ORDER_GAP
    previousOrder == null -> nextOrder!!.takeIf { it > 1 }?.div(2)
    nextOrder == null -> previousOrder + ORDER_GAP
    nextOrder - previousOrder >= 2 -> previousOrder + (nextOrder - previousOrder) / 2
    else -> null
  }

/**
 * Resolves the `order` for a step being inserted between [previousId] and [nextId].
 *
 * The lookups are passed in so the same logic serves recipe ingredients/instructions (versioned deltas) and
 * bake ingredients/instructions (plain rows):
 * - [activeOrders] returns id -> current order for the steps that are still active (not omitted).
 * - [rebalance] renumbers those steps with fresh gaps of [ORDER_GAP]; it is called at most once, when the
 *   neighbours have no integer room between them (e.g. orders 10 and 11).
 */
fun resolveInsertionOrder(
  entityLabel: String,
  previousParam: String,
  nextParam: String,
  previousId: Uuid?,
  nextId: Uuid?,
  activeOrders: () -> Map<Uuid, Int>,
  rebalance: () -> Unit,
): Int {
  val orders = activeOrders()
  val previousOrder =
    previousId?.let { orders[it] ?: throw NotFoundException(entityLabel, it.toString()) }
  val nextOrder =
    nextId?.let { orders[it] ?: throw NotFoundException(entityLabel, it.toString()) }

  if (previousOrder != null && nextOrder != null && previousOrder >= nextOrder) {
    throw BadRequestException("$previousParam must currently come before $nextParam")
  }

  computeMidpointOrder(previousOrder, nextOrder)?.let { return it }

  rebalance()
  val refreshedOrders = activeOrders()
  val refreshedPrevious = previousId?.let { refreshedOrders.getValue(it) }
  val refreshedNext = nextId?.let { refreshedOrders.getValue(it) }

  return computeMidpointOrder(refreshedPrevious, refreshedNext)
    ?: error("Unable to compute an insertion order for a new $entityLabel")
}
