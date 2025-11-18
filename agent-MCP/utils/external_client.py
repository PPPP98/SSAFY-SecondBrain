import httpx

class ExternalClient:
    def __init__(self):
        self.client = None

    async def get_client(self) -> httpx.AsyncClient:
        if self.client is None or self.client.is_closed:
            self.client = httpx.AsyncClient(timeout=60.0)
        return self.client

    async def close(self):
        if self.client is not None:
            await self.client.aclose()


external_client = ExternalClient()
