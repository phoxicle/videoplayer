<template>
  <div id="app">
    <video id="video" controls>
      <!-- mp4 and vtt from https://github.com/brenopolanski/html5-video-webvtt-example -->
      <source src="/MIB2.mp4" type="video/mp4" />
      <track id="subtitles" src="/MIB2-subtitles-pt-BR.vtt" kind="subtitles" srclang="en" label="English" default />
    </video>
    <div id="definition" v-if="hoveredWord">
      <strong>Definition: {{ hoveredWord }}</strong>: {{ definition }}
    </div>
  </div>
</template>

<script>
import { openDB } from 'idb';

export default {
  data() {
    return {
      hoveredWord: null,
      definition: null,
    };
  },
  async mounted() {
    const db = await openDB('WordDefinitionsDB', 1, {
      upgrade(db) {
        db.createObjectStore('definitions', { keyPath: 'word' });
      },
    });

    const video = document.getElementById('video');
    setInterval(async () => {
      const currentTime = Math.floor(video.currentTime);
      const startTime = currentTime;
      const endTime = currentTime + 5;

      const response = await fetch(`/getWordDefinitions?startTime=${startTime}&endTime=${endTime}`);
      const words = await response.json();

      const tx = db.transaction('definitions', 'readwrite');
      const store = tx.objectStore('definitions');
      for (const word of words) {
        store.put(word);
      }
      await tx.done;
    }, 5000);

    const track = document.getElementById('subtitles');
    track.addEventListener('cuechange', async (event) => {
      const activeCues = event.target.track.activeCues;
      if (activeCues.length > 0) {
        const text = activeCues[0].text;
        const words = text.split(' ');

        for (const word of words) {
          const tx = db.transaction('definitions', 'readonly');
          const store = tx.objectStore('definitions');
          const definition = await store.get(word);
          if (definition) {
            this.hoveredWord = word;
            this.definition = definition.definition;
            break;
          }
        }
      }
    });
  },
};
</script>

<style>
#definition {
  margin-top: 20px;
  font-size: 16px;
  color: #333;
}
</style>
