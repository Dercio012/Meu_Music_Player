# Meu Music Player (Android)

Um leitor de música nativo para Android desenvolvido em Java, focado em desempenho, funcionalidade e uma arquitetura limpa. O projeto segue o padrão MVP (Model-View-Presenter) e integra funcionalidades modernas, incluindo gestão de playlists locais, reprodução em segundo plano e reconhecimento de músicas.

## Funcionalidades Implementadas

* **Leitor de Música Completo:** Reprodução, Pausa, Próxima Faixa, Faixa Anterior.
* **Controles Avançados:** Modos Aleatório (Shuffle) e Repetir (None, One, All).
* **Reprodução em Segundo Plano:** Utiliza um `Foreground Service` para garantir que a música continue a tocar mesmo quando o aplicativo está minimizado ou a tela está desligada.
* **Notificação de Controle:** `MediaSessionCompat` e `NotificationCompat` integrados para exibir uma notificação com controles de reprodução (Play/Pause, Next, Prev).
* **Biblioteca de Músicas:** Uma `LibraryActivity` dedicada que carrega todas as faixas de áudio do dispositivo (`MediaStore`).
* **Busca em Tempo Real:** `SearchView` na barra de ferramentas da biblioteca para filtrar músicas por título ou artista instantaneamente.
* **Gestão de Playlists (com Room):**
    * Criação e remoção de playlists.
    * Adição de músicas a playlists através de um menu de contexto (três pontos) em cada música.
    * Visualização de músicas dentro de uma playlist (em `PlaylistDetailActivity`).
    * Reprodução de músicas a partir da lista de detalhes da playlist.
* **Animações:**
    * Rotação suave da capa do álbum (`ivAlbumArt`) durante a reprodução.
    * Transições de tela suaves (`slide-in`/`slide-out`) entre a `MainActivity` (Player) e as Activities `LibraryActivity` e `PlaylistsActivity`.
* **Reconhecimento de Música (ACRCloud):**
    * Integração com o SDK do ACRCloud.
    * Captura de áudio ambiente (`RECORD_AUDIO`) para identificar a música que está a tocar.
    * Exibição do resultado (Título e Artista) num diálogo.

## Arquitetura do Projeto

O projeto é construído sobre o padrão de arquitetura **MVP (Model-View-Presenter)**, combinado com um `Service` para a lógica de reprodução.

* **Model:** A camada de dados.
    * `model/Song.java`: Objeto simples (POJO) que representa uma faixa de música.
    * `database/`: Pacote que contém todas as classes da biblioteca **Room** (`AppDatabase`, `PlaylistDao`, `PlaylistEntity`, `PlaylistSongCrossRef`).
* **View:** A camada de UI (passiva).
    * `MainActivity.java`: Exibe o player principal ("Tocando Agora"). Implementa `MainContract.View`.
    * `LibraryActivity.java`: Exibe a lista de músicas, busca e opções de item.
    * `PlaylistsActivity.java`: Exibe a lista de playlists criadas.
    * `PlaylistDetailActivity.java`: Exibe as músicas de uma playlist específica.
* **Presenter:** O "cérebro" que liga a View e o Model.
    * `mvp/MainPresenter.java`: Implementa `MainContract.Presenter`. Gere a lógica da `MainActivity` (controlo do player, reconhecimento de música) e comunica com o `MusicService`.
    * *(Nota: `LibraryActivity` e `PlaylistsActivity` implementam a sua própria lógica de UI/Banco de Dados para simplificar, mas uma arquitetura mais complexa teria Presenters dedicados para elas também.)*
* **Service:**
    * `service/MusicService.java`: Um `Foreground Service` que gere o `MediaPlayer`, a fila de reprodução, o estado (Shuffle/Repeat), o `MediaSessionCompat` e as notificações de controlo.

## Bibliotecas Utilizadas

* **AndroidX AppCompat & Material:** Para componentes de UI modernos e retrocompatibilidade.
* **AndroidX RecyclerView:** Para exibição eficiente da lista de músicas e playlists.
* **AndroidX CardView:** Usado para a capa circular do álbum.
* **AndroidX Room:** Biblioteca de persistência para o banco de dados SQLite (gestão de playlists).
* **AndroidX Media (`androidx.media:media`):** Para `MediaSessionCompat` e controlo de notificações `MediaStyle`.
* **Glide:** Para carregamento eficiente de imagens (usado para as capas dos álbuns nas listas - *implementação futura*).
* **ACRCloud SDK (JAR local):** Para a funcionalidade de reconhecimento de música.

## Como Compilar

1.  Clone o repositório.
2.  Obtenha o SDK do ACRCloud para Android em [acrcloud.com](https://www.acrcloud.com/) (ex: `acrcloud-universal-sdk-x.x.x.jar`).
3.  Coloque o ficheiro `.jar` do SDK na pasta `app/libs` do projeto.
4.  Abra o arquivo `app/build.gradle.kts` e certifique-se que a linha `implementation(files("libs/NOME_DO_SEU_SDK.jar"))` corresponde ao nome do ficheiro.
5.  Abra `mvp/MainPresenter.java` e insira as suas credenciais do ACRCloud (`host`, `accessKey`, `accessSecret`) no método `setupACRCloud()`.
6.  Sincronize o Gradle e compile o projeto.
