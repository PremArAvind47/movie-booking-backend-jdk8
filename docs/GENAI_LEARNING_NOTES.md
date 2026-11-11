# GenAI Learning Notes (Spring Boot + Spring AI + Ollama)

Saved from two Claude sessions ("ga" and "gacode", 13 Sep 2026). Source: session transcripts
(user and assistant text). Not yet included: the local chat folder `C:\PremAravind\D--PAD4U`.

## Goal
One evolving project, `movie-booking-backend-jdk8`, used to learn Java + GenAI + Agentic AI.
Main AI is a **local Llama model through Ollama** (free, offline) instead of paid OpenAI.
Learning rule: the user wants to learn, so explain in **simple words, short answers**.
Often says "you do it" for code edits, but wants commands only when working in git.

## Environment facts
- Office laptop: 16 GB RAM, Intel 11th gen, no GPU, ~100 GB disk. Can run small models (3B-7B), CPU only.
  Close IntelliJ and heavy apps when running Ollama (free RAM is tight).
- Another PC (`D:\Prem_Aravind_AI\...`) reported 3.9 GB RAM + weak AMD GPU. Too small for Ollama.
- Laptop had only JDK 8. Installed Microsoft JDK 21 (`C:\Users\bluescopetech.naren\.jdks\ms-21.0.12.1`) via IntelliJ.
  - IDE Run button uses the Project SDK, so no JAVA_HOME change is needed there.
  - Terminals (including the IDE's Terminal tab) use system `JAVA_HOME`/PATH, which still points to JDK 8
    unless changed. For terminal Maven builds use e.g.
    `JAVA_HOME="C:\Users\bluescopetech.naren\.jdks\ms-21.0.12.1" mvn compile`.
- Stack: Spring Boot 4.1.1, Spring AI 2.0.1, Java 21 (`pom.xml`).
- Ollama 0.34.0, models: `llama3.2` (3.2B, ~2 GB, text only), `nomic-embed-text` (768 dims).
  Ollama API: `http://localhost:11434` (`curl http://localhost:11434/api/tags` to check).
- In `ollama run`, the `>>>` prompt is chat; type `/bye` to exit before running shell commands.

## Git history of the session
- Work was on `feature/movie-entity` (8 commits ahead of main: Jenkinsfile, Docker, hello endpoint...).
  Plan was to merge into `main` and keep working on `main`:
  `git checkout main && git pull origin main && git merge origin/feature/movie-entity && git push origin main`.
- Eclipse files (`.classpath`, `.project`, `.settings/*.prefs`) blocked checkout; some were moved to
  `%TEMP%\pad4u_ide_backup`. They are IDE files, safe to restore locally.

## Issues hit and fixes
| Problem | Cause | Fix |
|---|---|---|
| "Please add JDK 8 or later" in IntelliJ | pom needs Java 21, laptop only had JDK 8 | Install JDK 21, set Project SDK + language level 21 |
| Startup error "At least one credential source must be specified" | OpenAI starter in pom, no API key | Switched to the Ollama starter |
| `/ai` 500, `429 You have no credits remaining` | OpenAI account had $0 | Moved to local Ollama |
| API key in `application.properties` (other PC) | Plain-text `sk-proj-...` key | Rotate it, keep keys in env vars, never commit |

## 24-part plan and progress
0 Local AI setup (done) | 1 Java to local LLM (done) | 2 Prompts, context, tokens (done) |
3 Structured output (done) | 4 Embeddings (done) | 5 Vector database (coded + compiled, **test not confirmed**) |
6 Document processing/chunking | 7 RAG | 8 RAG engineering | 9 Memory | 10 Tool calling |
11 Agentic AI | 12 Multimodal | 13 Hallucination + grounding | 14 Guardrails + security |
15 Testing + evaluation | 16 Observability | 17 Performance | 18 Cost + model selection |
19 Cloud vs local | 20 Fine-tuning | 21 Production engineering | 22 Complete architecture |
23 End-to-end debugging | 24 Interview-level design.

### What each done part added
- **Part 1** pom: `spring-ai-starter-model-ollama`; `application.properties`:
  `spring.ai.ollama.base-url`, `spring.ai.ollama.chat.model=llama3.2`. `ChatClient` in `AiController`.
- **Part 2** `/ai?question=...&context=...`. System prompt = how the AI behaves; user prompt = the question;
  context = extra info pasted into the prompt (manual version of RAG). Test result: the AI used the context
  and answered "Interstellar".
- **Part 3** `MovieInfo` record + `/ai/movie-info?movieName=...`, using `.entity(MovieInfo.class)`.
  Returned `{"movie":"Interstellar","genre":"Science Fiction"}`.
- **Part 4** `spring.ai.ollama.embedding.model=nomic-embed-text`, `/ai/embed?text=...` returned 768 numbers.
- **Part 5** `VectorStoreConfig` (in-memory `SimpleVectorStore`, 5 sample movies, embedded at startup) and
  `/ai/search?query=...`. Next step is to run
  `http://localhost:8080/ai/search?query=space and time travel`. Expect Interstellar and/or Inception.
  Dependency added: `spring-ai-vector-store`.

## Concepts explained so far (simple words)
- **Llama is not code.** It is a ~2 GB file of numbers (weights). Ollama is the program that loads it and tells
  the CPU what math to run. No internet, no database, after the one-time download.
- **How an answer is made:** text, split into tokens, each token looked up as a number, numbers go through
  layers of math, model guesses the next token, add it, repeat until done.
- **Training:** the model guesses the next word over and over on huge text; wrong guesses nudge the weights.
  The training text is then thrown away; only the weights stay. Weights are like volume knobs.
- **GenAI** = generates new text word by word instead of looking up stored answers. It can hallucinate because it
  guesses from patterns and does not fact-check.
- **Tokens** = pieces of text. A model has a max (llama3.2: 131072 tokens) for question + context + answer.
- **Record** (Java 16+) = an immutable class with auto-generated constructor, getters, equals, hashCode, toString.
  This is why the project needs Java 21 and would not compile on Java 8.
- **Embedding** = text turned into a "location on a map of meaning" (768 numbers). Similar meaning means nearby.
  Vector DB stores (text, embedding) pairs and finds the closest ones to the question.
- `llama3.2` is text-only. `llama3.2-vision` handles images (planned for Part 12).

## Endpoints now in the app
| URL | Purpose |
|---|---|
| `/ai?question=..&context=..` | Chat with system prompt + optional context |
| `/ai/movie-info?movieName=..` | Structured output (`MovieInfo`) |
| `/ai/embed?text=..` | Show embedding size + first 5 values |
| `/ai/search?query=..` | Semantic search over 5 sample movies |

## Open items
1. Run and confirm the Part 5 `/ai/search` test, then start Part 6.
2. If the old OpenAI key is still live anywhere, rotate it.
3. Add the local chat folder `C:\PremAravind\D--PAD4U` to the repo (or paste it) so it can be reviewed too.
