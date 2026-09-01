-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

DROP EXTENSION pg_net;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT DELETE, INSERT, SELECT, UPDATE ON TABLES TO anon;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT, USAGE ON SEQUENCES TO anon;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON ROUTINES TO anon;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT DELETE, INSERT, SELECT, UPDATE ON TABLES TO authenticated;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT, USAGE ON SEQUENCES TO authenticated;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON ROUTINES TO authenticated;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT DELETE, INSERT, SELECT, UPDATE ON TABLES TO service_role;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT, USAGE ON SEQUENCES TO service_role;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON ROUTINES TO service_role;

CREATE TYPE public.rating_category AS ENUM (
  'OVERALL',
  'TASTE',
  'TEXTURE',
  'APPEARANCE',
  'RISE_STRUCTURE',
  'CUSTOM'
);

CREATE TABLE public.bake_ingredients (
  id                  uuid DEFAULT gen_random_uuid() NOT NULL,
  bake_id             uuid NOT NULL,
  ingredient_delta_id uuid NOT NULL,
  amount              text DEFAULT ''::text,
  name                text DEFAULT ''::text,
  notes               text
);

ALTER TABLE public.bake_ingredients
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bake_ingredients
  ADD CONSTRAINT bake_ingredients_pkey PRIMARY KEY (id);

GRANT ALL ON public.bake_ingredients TO anon;

GRANT ALL ON public.bake_ingredients TO authenticated;

GRANT ALL ON public.bake_ingredients TO service_role;

CREATE TABLE public.bake_instructions (
  id                   uuid DEFAULT gen_random_uuid() NOT NULL,
  bake_id              uuid NOT NULL,
  instruction_delta_id uuid NOT NULL,
  description          text DEFAULT ''::text,
  notes                text DEFAULT ''::text
);

ALTER TABLE public.bake_instructions
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bake_instructions
  ADD CONSTRAINT bake_instructions_pkey PRIMARY KEY (id);

GRANT ALL ON public.bake_instructions TO anon;

GRANT ALL ON public.bake_instructions TO authenticated;

GRANT ALL ON public.bake_instructions TO service_role;

CREATE TABLE public.bake_ratings (
  id             uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at     timestamp with time zone DEFAULT now() NOT NULL,
  bake_id        uuid,
  overall        smallint,
  taste          smallint,
  texture        smallint,
  appearance     smallint,
  rise_structure smallint,
  difficulty     smallint
);

ALTER TABLE public.bake_ratings
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bake_ratings
  ADD CONSTRAINT bake_ratings_pkey PRIMARY KEY (id);

GRANT ALL ON public.bake_ratings TO anon;

GRANT ALL ON public.bake_ratings TO authenticated;

GRANT ALL ON public.bake_ratings TO service_role;

CREATE TABLE public.bakes (
  id             uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at     timestamp with time zone DEFAULT now() NOT NULL,
  recipe_id      uuid                     NOT NULL,
  elevation      smallint,
  notes          text,
  start_datetime timestamp with time zone,
  end_datetime   timestamp with time zone
);

ALTER TABLE public.bakes
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bakes
  ADD CONSTRAINT "Bakes_pkey" PRIMARY KEY (id);

ALTER TABLE public.bake_ingredients
  ADD CONSTRAINT bake_ingredients_bake_id_fkey FOREIGN KEY (bake_id) REFERENCES public.bakes(id) ON DELETE CASCADE;

ALTER TABLE public.bake_instructions
  ADD CONSTRAINT bake_instructions_bake_id_fkey FOREIGN KEY (bake_id) REFERENCES public.bakes(id) ON DELETE CASCADE;

ALTER TABLE public.bake_ratings
  ADD CONSTRAINT bake_ratings_bake_id_fkey FOREIGN KEY (bake_id) REFERENCES public.bakes(id) ON UPDATE CASCADE ON DELETE CASCADE;

GRANT ALL ON public.bakes TO anon;

GRANT ALL ON public.bakes TO authenticated;

GRANT ALL ON public.bakes TO service_role;

CREATE TABLE public.ingredient_delta (
  id            uuid                     DEFAULT gen_random_uuid() NOT NULL,
  ingredient_id uuid                     DEFAULT gen_random_uuid(),
  version       smallint                 NOT NULL,
  amount        text                     NOT NULL,
  name          text                     NOT NULL,
  notes         text,
  created_at    timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.ingredient_delta
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.ingredient_delta
  ADD CONSTRAINT ingredient_delta_pkey PRIMARY KEY (id);

ALTER TABLE public.bake_ingredients
  ADD CONSTRAINT bake_ingredients_ingredient_delta_id_fkey FOREIGN KEY (ingredient_delta_id) REFERENCES public.ingredient_delta(id) ON UPDATE CASCADE ON DELETE CASCADE;

GRANT ALL ON public.ingredient_delta TO anon;

GRANT ALL ON public.ingredient_delta TO authenticated;

GRANT ALL ON public.ingredient_delta TO service_role;

CREATE TABLE public.ingredients (
  id           uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at   timestamp with time zone DEFAULT now() NOT NULL,
  recipe_id    uuid                     DEFAULT gen_random_uuid() NOT NULL,
  best_version smallint                 DEFAULT '1'::smallint NOT NULL,
  notes        text,
  "order"      smallint
);

ALTER TABLE public.ingredients
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.ingredients
  ADD CONSTRAINT ingredients_pkey PRIMARY KEY (id);

ALTER TABLE public.ingredient_delta
  ADD CONSTRAINT ingredient_delta_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES public.ingredients(id);

GRANT ALL ON public.ingredients TO anon;

GRANT ALL ON public.ingredients TO authenticated;

GRANT ALL ON public.ingredients TO service_role;

CREATE TABLE public.instruction_delta (
  id             uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at     timestamp with time zone DEFAULT now() NOT NULL,
  instruction_id uuid                     NOT NULL,
  description    text                     NOT NULL,
  notes          text,
  version        smallint                 NOT NULL
);

ALTER TABLE public.instruction_delta
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.instruction_delta
  ADD CONSTRAINT instruction_delta_pkey PRIMARY KEY (id);

ALTER TABLE public.bake_instructions
  ADD CONSTRAINT bake_instructions_instruction_delta_id_fkey FOREIGN KEY (instruction_delta_id) REFERENCES public.instruction_delta(id) ON UPDATE CASCADE ON DELETE CASCADE;

GRANT ALL ON public.instruction_delta TO anon;

GRANT ALL ON public.instruction_delta TO authenticated;

GRANT ALL ON public.instruction_delta TO service_role;

CREATE TABLE public.instructions (
  id           uuid                     DEFAULT gen_random_uuid() NOT NULL,
  recipe_id    uuid                     DEFAULT gen_random_uuid() NOT NULL,
  best_version smallint                 DEFAULT '1'::smallint NOT NULL,
  notes        text,
  created_at   timestamp with time zone DEFAULT now() NOT NULL,
  "order"      smallint
);

ALTER TABLE public.instructions
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.instructions
  ADD CONSTRAINT instructions_pkey PRIMARY KEY (id);

ALTER TABLE public.instruction_delta
  ADD CONSTRAINT instruction_delta_instruction_id_fkey FOREIGN KEY (instruction_id) REFERENCES public.instructions(id);

GRANT ALL ON public.instructions TO anon;

GRANT ALL ON public.instructions TO authenticated;

GRANT ALL ON public.instructions TO service_role;

CREATE TABLE public.recipes (
  id                 uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at         timestamp with time zone DEFAULT now() NOT NULL,
  name               text                     NOT NULL,
  description        text,
  recipe_source      text,
  tags               text[],
  tools              text[],
  refined            boolean                  DEFAULT false NOT NULL,
  notes              text,
  recipe_source_type text,
  favorite           boolean                  DEFAULT false NOT NULL,
  difficulty_rating  smallint
);

ALTER TABLE public.recipes
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.recipes
  ADD CONSTRAINT recipes_pkey PRIMARY KEY (id);

ALTER TABLE public.bakes
  ADD CONSTRAINT "Bakes_recipe_id_fkey" FOREIGN KEY (recipe_id) REFERENCES public.recipes(id) ON DELETE CASCADE;

ALTER TABLE public.ingredients
  ADD CONSTRAINT ingredients_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(id);

ALTER TABLE public.instructions
  ADD CONSTRAINT instructions_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(id);

GRANT ALL ON public.recipes TO anon;

GRANT ALL ON public.recipes TO authenticated;

GRANT ALL ON public.recipes TO service_role;
