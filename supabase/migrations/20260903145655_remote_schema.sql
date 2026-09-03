-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.ingredients
  DROP COLUMN "order";

ALTER TABLE public.ingredients
  DROP COLUMN notes;

ALTER TABLE public.instructions
  DROP COLUMN "order";

ALTER TABLE public.instructions
  DROP COLUMN notes;

ALTER TABLE public.bake_ingredients
  ADD COLUMN "order" smallint;

ALTER TABLE public.bake_instructions
  ADD COLUMN "order" smallint;

ALTER TABLE public.ingredient_delta
  ADD COLUMN "order" smallint;

ALTER TABLE public.instruction_delta
  ADD COLUMN "order" smallint;
