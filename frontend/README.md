# Revisa+ Frontend

Frontend da plataforma gamificada **Revisa+** desenvolvido em Angular 21.

## 🚀 Tecnologias

- Angular 21
- TypeScript 5.9
- SCSS
- Angular Material
- Bootstrap 5
- RxJS 7
- Standalone Components

## 📁 Estrutura do Projeto

```
src/app/
├── core/                    # Núcleo da aplicação
│   ├── models/             # Interfaces e tipos
│   ├── services/           # Serviços (API, Auth, etc)
│   ├── guards/             # Guards de rota
│   ├── interceptors/       # HTTP Interceptors
│   └── core.module.ts      # Módulo core
├── features/               # Módulos de funcionalidades
│   ├── auth/              # Autenticação
│   ├── dashboard/         # Dashboard principal
│   ├── trails/            # Trilhas de aprendizagem
│   ├── activities/        # Atividades e quizzes
│   ├── gamification/      # Badges, ranking, missões
│   └── admin/             # Painel administrativo
├── shared/                # Componentes compartilhados
│   ├── components/        # Componentes reutilizáveis
│   ├── directives/        # Diretivas customizadas
│   └── pipes/             # Pipes customizados
└── app.ts                 # Componente raiz
```

## 🏃 Servidor de Desenvolvimento

Para iniciar o servidor local, execute:

```bash
npm install
npm start
```

Acesse `http://localhost:4200/`. A aplicação recarrega automaticamente ao modificar arquivos.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
