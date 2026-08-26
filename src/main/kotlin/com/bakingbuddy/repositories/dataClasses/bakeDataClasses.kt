import kotlin.uuid.Uuid

data class BestIngredientDelta(
  val deltaId: Uuid,
  val ingredientId: Uuid,
  val version: Int,
  val amount: String,
  val name: String,
)

data class BestInstructionDelta(
  val deltaId: Uuid,
  val instructionId: Uuid,
  val version: Int,
  val description: String,
)