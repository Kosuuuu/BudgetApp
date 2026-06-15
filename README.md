# BudgetApp

Aplikacja budżetowa wykonana w ramach laboratoriów PASiR Java.

## Autor

Jakub Kosecki

## Struktura projektu

- `backend` – aplikacja Spring Boot z GraphQL, JWT, JPA i MySQL
- `frontend` – aplikacja React/Vite
- `docker-compose.yml` – uruchomienie MySQL, backendu i frontendu w kontenerach

## Funkcjonalności

- rejestracja i logowanie użytkownika
- autoryzacja JWT
- transakcje przychodów i wydatków
- grupy użytkowników
- długi grupowe
- oznaczanie i potwierdzanie spłat
- wybór uczestników wydatku grupowego
- powiadomienia WebSocket