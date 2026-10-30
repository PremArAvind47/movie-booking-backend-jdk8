# CLAUDE.md

Read this whole file at the start of every session. Keep it updated (progress, preferences, problems and fixes) and push to `main`.
Key rules: be honest, use simple words and short answers, commit directly to `main`.

## Claude's memory (read this first)
This file is Claude's memory for this repo. Update it whenever something worth remembering comes up,
then commit and push directly to `main`.

**How to work with this user**
- Be honest and straightforward. Say plainly what was and wasn't done or read. No guessing presented as fact.
- Use simple words and short answers. The user says English is not their strongest language. Avoid heavy jargon.
- The user is here to learn. When they say "you do it", make the edit. Otherwise explain and let them do the step.
- When working in git, give commands only if asked. Commit and push directly to `main` (the user said so).
  Don't create other branches or PRs unless asked.
- Before editing code, say what changed. The user wants to see what changed.
- **A part is only DONE when (1) the code works AND (2) the user says they understand it.** Never move to the next part just because the code runs. Explain, ask a check question, and wait for the user to confirm.
- Lesson learned: the user gets lost when I explain library internals. Explain only what THEY need, with everyday analogies, and check understanding with a question in their own words. Always compile-check or say "not compiled" before giving code (I once gave wrong code: `vectorStore.add(movie)` needs a List).
- Test lesson: the store is in memory and is reloaded automatically at every app start (since Part 8; before that `/ai/load-file` had to be called). A test is only meaningful if the answer can NOT come from the model's own training (I wrongly said RAG worked on a Titanic question Llama already knew).
- **Do only what the plan needs. No extra endpoints, features or "bonus" code unless the user asks.** (User said this on 22 Oct 2026 after I added extra endpoints in Part 6.)
- A session may be told to use a specific branch. If it differs from `main`, tell the user and follow their latest instruction.

**Environment limits learned**
- Cloud sessions can't see the user's local folders (`D:\PAD4U`, `C:\PremAravind\D--PAD4U`). Anything local must be pushed to the repo first.
- Cloud sessions can't run the app against the user's Ollama (it runs on their laptop).
- Deleting a remote branch from the cloud session failed twice ("remote end hung up"). The user must delete it on GitHub.
- Other Claude sessions can be read with the session tools. The older sessions were `session_01R4N35QexvKTpzLcSQ5mj37` ("ga")
  and `session_01LeGxRFxQk7s3GYeCh8TEAg` ("gacode"). Read them with `list_events`, filtered to `user` and `assistant`.
  Results are huge, so save to a file and extract the text with a script.

**Status**
- Parts 0-4: done. Part 5: DONE (code tested 12 Oct 2026; user confirmed they understand, 21 Oct 2026). They explained it back correctly (store built with embeddingModel, query becomes numbers, closest stored movies returned). Part 6: DONE (user confirmed 22 Oct 2026). Added: movies.txt in resources, TextReader, paragraph chunks with metadata, /ai/load-file stores chunks (call once per app start). Extras were removed at the user's request. Part 7 (RAG): DONE (user confirmed 22 Oct 2026). `/ai/ask?question=...` = similaritySearch, join chunk text, system+user prompt to Llama. Verified with a question only the file answers ("one hour = seven years") AFTER calling /ai/load-file. Known weakness for Part 8: llama3.2 (3B) can garble facts and ignore "use only the information"; default search returns 4 chunks incl. irrelevant ones. Part 11 (Agentic AI): code pushed 30 Oct 2026. REAL TEST with llama3.2 FAILED (console: `TOOL getFreeSeats called with movieName=getMovieList` - it used a tool name as the movie name, never called getMovieList, wrong final answer); the code itself is fine. RETEST with `qwen2.5:7b` PASSED on 30 Oct 2026 (/ai/agent answered Batman, 200 rupees, 7 seats using the tool data; took 1m26s on the laptop; note: the first retest still used llama3.2 because the model line had not reached the laptop - check `ollama ps`). Chat model switched to `qwen2.5:7b` in application.properties on 30 Oct 2026 to retest (user must `ollama pull qwen2.5:7b` first; if it is too slow/wrong, options: cloud model (~$1-5/month rough guess, Part 19) or go back to llama3.2 by editing spring.ai.ollama.chat.model), Part 12 (Multimodal): code pushed 31 Oct 2026. REAL TEST PASSED 30 Oct 2026 (user's laptop, qwen2.5vl:3b read my generated ticket picture: Batman, A5, "200 rupees"; 1m20s incl. model load). Part 12 DONE (user confirmed 30 Oct 2026). Understood: .user(text + .media(type, picture)) = what we give the AI; .options(...model(visionModel)) picks the seeing model for that one call; system = general rules, user = this request; the 10MB multipart limit in application.properties is applied by Spring before the method runs (default 1MB). `POST /ai/read-ticket` (multipart field `image`) sends the picture to the vision model `vision.model=qwen2.5vl:3b` (~3.2 GB, user must `ollama pull qwen2.5vl:3b`; chosen from web search, sizes approximate) using `.options(OllamaChatOptions.builder().model(visionModel))` for that call only (other endpoints keep qwen2.5:7b), and returns `TicketInfo(movieName, seat, price)` via `.entity(TicketInfo.class)`. multipart limit raised to 10MB (default is 1MB). Tested here with a fake Ollama only (image arrived, right model name, JSON parsed); real vision model quality NOT tested (small vision models can misread text). PDFs NOT done (needs extra dependency). Part 11 DONE (user confirmed 30 Oct 2026). Understood: an agent = tools + the loop Spring AI repeats (ask tool -> result -> think again) + a step-by-step system instruction; the instruction alone does not make it an agent; real agents also need max steps, guardrails (Part 14). Speed ideas not coded: tools that return more per call (fewer steps), keep-alive, cloud model. Added third tool `getMovieList()` in MovieTools and `/ai/agent?task=..` (system rule: solve step by step, use tools as many times as needed, never guess). Spring AI repeats the tool loop by itself. Tested here with a fake Ollama that scripted 5 sequential tool calls (the loop runs); real llama3.2 multi-step reasoning NOT tested (3B model may skip steps; consider a bigger model). Part 10 (Tool calling): DONE (user confirmed understanding 30 Oct 2026; I never saw their real-Llama test result, so real llama3.2 tool choice is still unverified). Understood: .tools(movieTools) gives Llama a menu of the @Tool methods; Llama picks by the descriptions (@Tool and @ToolParam) and only ASKS; our Java runs the method and returns the result; if no tool fits Llama answers from its own knowledge. `tools/MovieTools.java` (@Component, @Tool methods getFreeSeats(movieName), getTicketPrice(movieName); sample data Avengers/Batman/Dune: seats 42/7/120, prices 250/200/300; logs "TOOL ... called"). `/ai/tools?question=..` = chatClient with system rule + `.tools(movieTools)`. Plumbing tested here with a fake Ollama that asks for tools (tools offered, Java ran, result returned); real llama3.2 tool choice NOT tested (small model may ignore/misuse tools; if so consider a bigger model via `ollama pull` + spring.ai.ollama.chat.model). Part 9 (Memory): DONE (user confirmed 29 Oct 2026; I never saw their /ai/chat test results). Understood: memoryAdvisor reads/writes ChatMemory by conversationId; window = last 10 messages (memory.max-messages; library default is 20); in-RAM so lost on restart; long-term memory idea = save chat in vector store and retrieve related messages (NOT coded, user did not ask for it). `MemoryConfig` = `MessageWindowChatMemory` (memory.max-messages=10 in application.properties, in-memory, lost on restart). `/ai/chat?conversationId=..&message=..` = plain chat with memory. `/ai/ask?question=..&conversationId=..` (conversationId optional) = RAG + memory; the retrieved info is passed in the SYSTEM message (param) so it is not saved in memory. Tested here with a fake Ollama mock (memory, separate conversations, window drop all verified). NOT coded: long-term memory (persist across restarts, e.g. JDBC/file repository) - offered to the user. Known limit: with RAG, a follow-up like "who directed it?" finds no chunk (retrieval does not use memory) -> "I don't know". Part 8 (RAG engineering): DONE (user confirmed 26 Oct 2026). User saw the unrelated question return "I don't know" (59 ms, Llama not called); I never saw their result for the file question (seven years), so rag.min-score=0.5 is still unverified for good questions. The file now loads AUTOMATICALLY at app startup (@EventListener ApplicationReadyEvent in AiController, logs "Loaded 4 chunks"); `/ai/load-file` was REMOVED (no need to call anything after restart). `/ai/ask` now uses SearchRequest: topK (rag.top-k=2), similarityThreshold (rag.min-score=0.5, a GUESS, not tuned with real nomic-embed-text; user must tune in application.properties), metadata filter `source == 'movies.txt'` (excludes the Part 5 sample sentences), returns "I don't know" without calling Llama if no chunk passes, system prompt also says say "I don't know". Scores are logged (RAG chunk score=...). Whole app (startup load + /ai/ask + I don't know) tested here against a fake Ollama mock only; real nomic/llama behaviour NOT tested.
- Test result: Part 5 `/ai/search?query=space and time travel` returned Interstellar, Inception, Titanic, Dark Knight (default top 4 of 5). 
- User tests endpoints in Postman (collection `personalProject`, folder `ga`).
- Chats saved on the user's machine in `C:\PremAravind\D--PAD4U` are NOT yet reviewed.
- Leftover: branch `claude/chat-review-p0to71` still exists on GitHub (same commit is on `main`). The user should delete it.

# Project notes: GenAI Learning (Spring Boot + Spring AI + Ollama)

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
3 Structured output (done) | 4 Embeddings (done) | 5 Vector database (done, understood) |
6 Document processing/chunking (done, understood) | 7 RAG (done, understood) | 8 RAG engineering (done, understood) | 9 Memory (done, understood) | 10 Tool calling (done, understood) | 11 Agentic AI (done, understood) |
12 Multimodal (done, understood) | 13 Hallucination + grounding (next) | 14 Guardrails + security |
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
- **Part 6** `src/main/resources/movies.txt`, `TextReader` reads it, `splitFileIntoChunks()` cuts it at blank lines into `Document`s with metadata (source, chunkNumber), `/ai/load-file` adds them to the vector store.
- **Part 7** `/ai/ask`: RAG. Find chunks -> join text -> chatClient system rule + user (information + question) -> answer.
- **Part 12** `TicketInfo` record + `POST /ai/read-ticket` (MultipartFile image -> `.user(u -> u.text(..).media(mimeType, resource))` + vision model option + `.entity(TicketInfo.class)`).
- **Part 11** third tool `getMovieList` + `/ai/agent` (stronger step-by-step system rule; same `.tools(movieTools)` loop).
- **Part 10** `MovieTools` (@Tool getFreeSeats, getTicketPrice) + `/ai/tools` using `.tools(movieTools)`.
- **Part 9** `MemoryConfig` (MessageWindowChatMemory, window = memory.max-messages) + `MessageChatMemoryAdvisor` in AiController; `/ai/chat` and optional `conversationId` on `/ai/ask`.
- **Part 8** `/ai/ask` upgraded: SearchRequest with topK + similarityThreshold + filterExpression on metadata `source`; settings `rag.top-k`, `rag.min-score` in application.properties; no close chunk -> "I don't know".
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
| `/ai/search?query=..` | Semantic search over the stored movies and file chunks |
| `/ai/read-file` | Read `movies.txt` (1 Document, character count) |
| `/ai/chunks` | Show the paragraph chunks of `movies.txt` |
| `POST /ai/read-ticket` (form-data `image`) | Multimodal: picture of a movie ticket -> `TicketInfo` (movieName, seat, price) via vision model |
| `/ai/agent?task=..` | Agent: Llama uses several tools one after another (getMovieList, getTicketPrice, getFreeSeats) |
| `/ai/tools?question=..` | Tool calling: Llama may call getFreeSeats / getTicketPrice (Java methods) |
| `/ai/chat?conversationId=..&message=..` | Chat with short-term memory (per conversationId) |
| `/ai/ask?question=..&conversationId=..` | RAG: search file chunks (top-K, min score, source filter), give them to Llama, "I don't know" if nothing close |

## Open items
1. Start Part 13 (Hallucination + grounding). Known: llama3.2 (3B) fails multi-step tasks; qwen2.5:7b is slow (~1.5 min per agent task). If good questions ever get "I don't know", lower rag.min-score in application.properties.
2. If the old OpenAI key is still live anywhere, rotate it.
3. Add the local chat folder `C:\PremAravind\D--PAD4U` to the repo (or paste it) so it can be reviewed too.
