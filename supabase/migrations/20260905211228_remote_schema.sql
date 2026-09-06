-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

CREATE TABLE public.bake_images (
  id         uuid                     DEFAULT gen_random_uuid() NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  bake_id    uuid                     NOT NULL,
  path       text                     NOT NULL
);

ALTER TABLE public.bake_images
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.bake_images
  ADD CONSTRAINT bake_images_bake_id_fkey FOREIGN KEY (bake_id) REFERENCES public.bakes(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE public.bake_images
  ADD CONSTRAINT bake_images_pkey PRIMARY KEY (id);

GRANT ALL ON public.bake_images TO anon;

GRANT ALL ON public.bake_images TO authenticated;

GRANT ALL ON public.bake_images TO service_role;
