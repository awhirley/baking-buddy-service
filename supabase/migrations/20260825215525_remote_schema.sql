-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.bakes
  DROP COLUMN date;

ALTER TABLE public.bakes
  DROP COLUMN results;

ALTER TABLE public.bake_ingredients
  DROP CONSTRAINT bake_ingredients_ingredient_delta_id_fkey;

ALTER TABLE public.bake_ingredients
  DROP COLUMN ingredient_delta_id;

ALTER TABLE public.bake_instructions
  DROP CONSTRAINT bake_instructions_instruction_delta_id_fkey;

ALTER TABLE public.bake_instructions
  DROP COLUMN instruction_delta_id;

ALTER TABLE public.bake_instructions
  ALTER COLUMN bake_id SET NOT NULL;

ALTER TABLE public.bake_ingredients
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bake_ingredients
  ADD COLUMN ingredient_id uuid NOT NULL;

ALTER TABLE public.bake_ingredients
  ADD CONSTRAINT bake_ingredients_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES public.ingredients(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE public.bake_ingredients
  ADD COLUMN amount text DEFAULT 'NULL'::text;

ALTER TABLE public.bake_ingredients
  ADD COLUMN name text DEFAULT 'NULL'::text;

ALTER TABLE public.bake_ingredients
  ADD COLUMN notes text;

ALTER TABLE public.bake_instructions
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bake_instructions
  ADD COLUMN instruction_id uuid NOT NULL;

ALTER TABLE public.bake_instructions
  ADD CONSTRAINT bake_instructions_instruction_id_fkey FOREIGN KEY (instruction_id) REFERENCES public.instructions(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE public.bake_instructions
  ADD COLUMN description text DEFAULT 'NULL'::text;

ALTER TABLE public.bake_instructions
  ADD COLUMN notes text DEFAULT 'NULL'::text;

ALTER TABLE public.bakes
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bakes
  ADD COLUMN start_datetime timestamp with time zone;

ALTER TABLE public.bakes
  ADD COLUMN end_datetime timestamp with time zone;

ALTER TABLE public.ingredient_delta
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.ingredients
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.ingredients
  ADD COLUMN "order" smallint;

ALTER TABLE public.instruction_delta
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.instructions
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.instructions
  ADD COLUMN "order" smallint;

ALTER TABLE public.recipes
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.recipes
  ADD COLUMN recipe_source_type text;
