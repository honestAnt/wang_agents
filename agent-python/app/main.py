"""Enterprise AI Platform — Agent Runtime entry point."""

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.chat import router as chat_router
from app.trace.tracer import init_tracer


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_tracer()
    yield


app = FastAPI(
    title="Enterprise AI Platform — Agent Runtime",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["X-Trace-Id"],
)

app.include_router(chat_router, prefix="/api/v1")


@app.get("/health")
async def health():
    return {"status": "healthy", "service": "agent-runtime"}
