-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

CREATE TABLE public.bake_ingredients (
  id                  uuid DEFAULT gen_random_uuid() NOT NULL,
  bake_id             uuid NOT NULL,
  ingredient_delta_id uuid NOT NULL
);

ALTER TABLE public.bake_ingredients
  ADD CONSTRAINT bake_ingredients_ingredient_delta_id_fkey FOREIGN KEY (ingredient_delta_id) REFERENCES public.ingredient_delta(id) ON DELETE CASCADE;

ALTER TABLE public.bake_ingredients
  ADD CONSTRAINT bake_ingredients_pkey PRIMARY KEY (id);

GRANT ALL ON public.bake_ingredients TO anon;

GRANT ALL ON public.bake_ingredients TO authenticated;

GRANT ALL ON public.bake_ingredients TO service_role;

CREATE TABLE public.bake_instructions (
  id                   uuid DEFAULT gen_random_uuid() NOT NULL,
  bake_id              uuid,
  instruction_delta_id uuid
);

ALTER TABLE public.bake_instructions
  ADD CONSTRAINT bake_instructions_instruction_delta_id_fkey FOREIGN KEY (instruction_delta_id) REFERENCES public.instruction_delta(id) ON DELETE CASCADE;

ALTER TABLE public.bake_instructions
  ADD CONSTRAINT bake_instructions_pkey PRIMARY KEY (id);

GRANT ALL ON public.bake_instructions TO anon;

GRANT ALL ON public.bake_instructions TO authenticated;

GRANT ALL ON public.bake_instructions TO service_role;

CREATE TABLE public.bakes (
  id         uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  recipe_id  uuid                     NOT NULL,
  date       date,
  results    text,
  elevation  smallint,
  notes      text
);

ALTER TABLE public.bakes
  ADD CONSTRAINT "Bakes_pkey" PRIMARY KEY (id);

ALTER TABLE public.bake_ingredients
  ADD CONSTRAINT bake_ingredients_bake_id_fkey FOREIGN KEY (bake_id) REFERENCES public.bakes(id) ON DELETE CASCADE;

ALTER TABLE public.bake_instructions
  ADD CONSTRAINT bake_instructions_bake_id_fkey FOREIGN KEY (bake_id) REFERENCES public.bakes(id) ON DELETE CASCADE;

ALTER TABLE public.bakes
  ADD CONSTRAINT "Bakes_recipe_id_fkey" FOREIGN KEY (recipe_id) REFERENCES public.recipes(id) ON DELETE CASCADE;

GRANT ALL ON public.bakes TO anon;

GRANT ALL ON public.bakes TO authenticated;

GRANT ALL ON public.bakes TO service_role;
