<template>
  <div id="app">
    <video id="video" controls>
      <!-- mp4 and vtt from https://github.com/brenopolanski/html5-video-webvtt-example -->
      <source src="/MIB2.mp4" type="video/mp4" />
      <track id="subtitles" src="/MIB2-subtitles-pt-BR.vtt" kind="subtitles" srclang="en" label="English" default />
    </video>
    <div id="custom-subtitles"></div>
    <div id="definition" v-if="hoveredWord">
      <strong>{{ hoveredWord }}</strong>: {{ definition }}
    </div>
  </div>
</template>

<script>
import { openDB } from 'idb';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
console.log(`API Base URL: ${apiBaseUrl}`);

export default {
  data() {
    return {
      hoveredWord: null,
      definition: null,
      requestedTimeRanges: new Set()
    };
  },
  async mounted() {
    const db = await openDB('WordDefinitionsDB', 1, {
      upgrade(db) {
        db.createObjectStore('definitions', { keyPath: 'word' });
      },
    });

    const video = document.getElementById('video');
    const customSubtitles = document.getElementById('custom-subtitles');

    setInterval(async () => {
      const currentTime = Math.floor(video.currentTime);
      const startTime = currentTime;
      const endTime = currentTime + 5;

      const rangeKey = `${startTime}-${endTime}`;
      if (this.requestedTimeRanges.has(rangeKey)) {
        console.log(`Skipping API request for range: ${rangeKey}`);
        return;
      }

      const url = `${apiBaseUrl}/getWordDefinitions?startTime=${startTime}&endTime=${endTime}`;
      console.log(`Fetching URL: ${url}`);
      const response = await fetch(url);
      const words = await response.json();

      const tx = db.transaction('definitions', 'readwrite');
      const store = tx.objectStore('definitions');
      for (const word of words) {
        console.log("Adding word:", word)
        store.put(word);
      }

      this.requestedTimeRanges.add(rangeKey);

      await tx.done;
    }, 5000);

    const track = document.getElementById('subtitles');
    track.addEventListener('cuechange', async (event) => {
      console.log("cuechange event triggered");
      const activeCues = event.target.track.activeCues;
      if (activeCues.length > 0) {
        // TODO improve tokenization, reuse from server -->
        const text = activeCues[0].text;
        const words = text.split(' ');

        customSubtitles.innerHTML = ''; // Clear previous subtitles
        for (const word of words) {
          const span = document.createElement('span');
          span.textContent = word;
          span.className = 'subtitle-word';
          span.addEventListener('mouseover', async () => {
            console.log("Word hovered: ", word);
            const tx = db.transaction('definitions', 'readonly');
            const store = tx.objectStore('definitions');
            const definition = await store.get(word);
            if (definition) {
              this.hoveredWord = word;
              this.definition = definition.definition;
            }
          });
          span.addEventListener('mouseout', () => {
            this.hoveredWord = null;
            this.definition = null;
          });
          customSubtitles.appendChild(span);
          customSubtitles.appendChild(document.createTextNode(' ')); // Add space between words
        }
      }
    });
  },
};
</script>
