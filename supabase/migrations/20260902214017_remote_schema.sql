-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.bake_ingredients
  ADD COLUMN completed_bake_delta_id uuid;

ALTER TABLE public.bake_instructions
  ADD COLUMN completed_bake_delta_id uuid;

ALTER TABLE public.ingredient_delta
  ADD COLUMN source_bake_id uuid;

ALTER TABLE public.instruction_delta
  ADD COLUMN source_bake_id uuid;
