-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.bake_ingredients
  DROP CONSTRAINT bake_ingredients_ingredient_id_fkey;

ALTER TABLE public.bake_ingredients
  DROP COLUMN ingredient_id;

ALTER TABLE public.bake_instructions
  DROP CONSTRAINT bake_instructions_instruction_id_fkey;

ALTER TABLE public.bake_instructions
  DROP COLUMN instruction_id;

ALTER TABLE public.bake_ingredients
  ADD COLUMN ingredient_delta_id uuid NOT NULL;

ALTER TABLE public.bake_ingredients
  ADD CONSTRAINT bake_ingredients_ingredient_delta_id_fkey FOREIGN KEY (ingredient_delta_id) REFERENCES public.ingredient_delta(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE public.bake_instructions
  ADD COLUMN instruction_delta_id uuid NOT NULL;

ALTER TABLE public.bake_instructions
  ADD CONSTRAINT bake_instructions_instruction_delta_id_fkey FOREIGN KEY (instruction_delta_id) REFERENCES public.instruction_delta(id) ON UPDATE CASCADE ON DELETE CASCADE;
