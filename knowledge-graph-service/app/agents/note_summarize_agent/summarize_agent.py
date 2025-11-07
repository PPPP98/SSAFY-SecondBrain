from langchain.chat_models import init_chat_model
from app.core.config import get_settings

settings = get_settings()

model = init_chat_model(
    model=settings.summarize_model,
    temperature=settings.summarize_temperature,
)


test = model.invoke("Hello")

print(test)  # 테스트 출력