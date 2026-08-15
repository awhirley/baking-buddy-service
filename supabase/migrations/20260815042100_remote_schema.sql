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
  ADD CONSTRAINT ingredient_delta_pkey PRIMARY KEY (id);

GRANT ALL ON public.ingredient_delta TO anon;

GRANT ALL ON public.ingredient_delta TO authenticated;

GRANT ALL ON public.ingredient_delta TO service_role;

CREATE TABLE public.ingredients (
  id           uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at   timestamp with time zone DEFAULT now() NOT NULL,
  recipe_id    uuid                     DEFAULT gen_random_uuid() NOT NULL,
  best_version smallint                 DEFAULT '1'::smallint NOT NULL,
  notes        text
);

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
  ADD CONSTRAINT instruction_delta_pkey PRIMARY KEY (id);

GRANT ALL ON public.instruction_delta TO anon;

GRANT ALL ON public.instruction_delta TO authenticated;

GRANT ALL ON public.instruction_delta TO service_role;

CREATE TABLE public.instructions (
  id           uuid                     DEFAULT gen_random_uuid() NOT NULL,
  recipe_id    uuid                     DEFAULT gen_random_uuid() NOT NULL,
  best_version smallint                 DEFAULT '1'::smallint NOT NULL,
  notes        text,
  created_at   timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.instructions
  ADD CONSTRAINT instructions_pkey PRIMARY KEY (id);

ALTER TABLE public.instruction_delta
  ADD CONSTRAINT instruction_delta_instruction_id_fkey FOREIGN KEY (instruction_id) REFERENCES public.instructions(id);

GRANT ALL ON public.instructions TO anon;

GRANT ALL ON public.instructions TO authenticated;

GRANT ALL ON public.instructions TO service_role;

CREATE TABLE public.recipes (
  id            uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at    timestamp with time zone DEFAULT now() NOT NULL,
  name          text                     NOT NULL,
  description   text,
  recipe_source text,
  tags          text[],
  tools         text[],
  refined       boolean                  DEFAULT false NOT NULL,
  notes         text
);

ALTER TABLE public.recipes
  ADD CONSTRAINT recipes_pkey PRIMARY KEY (id);

ALTER TABLE public.ingredients
  ADD CONSTRAINT ingredients_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(id);

ALTER TABLE public.instructions
  ADD CONSTRAINT instructions_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(id);

GRANT ALL ON public.recipes TO anon;

GRANT ALL ON public.recipes TO authenticated;

GRANT ALL ON public.recipes TO service_role;
