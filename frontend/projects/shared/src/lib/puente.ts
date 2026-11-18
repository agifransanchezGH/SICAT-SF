import { Component } from '@angular/core';

/**
 * Silueta del Puente Colgante sobre la Setúbal, en línea: dos pares de torres, la catenaria de los cables,
 * las péndolas y la línea del agua. Es el único gesto gráfico de la identidad (ADR 0016); va decorativo.
 */
@Component({
  selector: 'sicat-puente',
  template: `
    <svg viewBox="0 0 1200 110" preserveAspectRatio="xMidYMax meet" aria-hidden="true" focusable="false">
      <path class="agua" d="M0 98 H1200" />
      <path d="M360 98 V22 M376 98 V22 M824 98 V22 M840 98 V22" />
      <path d="M360 26 H376 M824 26 H840 M360 42 H376 M824 42 H840" />
      <path class="cable" d="M40 98 Q210 32 368 24 Q600 98 832 24 Q990 32 1160 98" />
      <path d="M40 82 H1160" />
      <path class="pendolas" d="M300 82 V54 M420 82 V44 M480 82 V62 M540 82 V74 M600 82 V79 M660 82 V74 M720 82 V62 M780 82 V44 M900 82 V54" />
      <path class="agua" d="M120 98 Q140 91 160 98 M1000 98 Q1020 91 1040 98 M560 104 Q580 98 600 104" />
    </svg>
  `,
  styles: `
    :host { display: block; pointer-events: none; aspect-ratio: 1200 / 110; }
    svg { display: block; width: 100%; height: 100%; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; }
    .agua { opacity: 0.4; }
    .pendolas { stroke-width: 1.2; opacity: 0.8; }
    /* El cable se dibuja una vez al cargar la portada: el único movimiento que no responde a una acción. */
    .cable { stroke-dasharray: 1400; stroke-dashoffset: 1400; animation: tender 1.4s var(--sicat-curva) 0.15s forwards; }
    @keyframes tender { to { stroke-dashoffset: 0; } }
    @media (prefers-reduced-motion: reduce) { .cable { animation: none; stroke-dashoffset: 0; } }
  `,
})
export class Puente {}
