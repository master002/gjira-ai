package com.gjira.ingest.vectorization;

import org.springframework.stereotype.Service;

/**
 * Produces embeddings for chunk text.
 * Dev: returns placeholder (zeros). Production: integrate OpenAI/Vertex/本地 model.
 */
@Service
public class EmbeddingService {

    private static final int DIM = 1536;

    public float[] embed(String text) {
        float[] v = new float[DIM];
        for (int i = 0; i < DIM; i++) {
            v[i] = 0f;
        }
        return v;
    }
}
