import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import type { Story } from '@/src/api/types';

function splitPages(content: string): string[] {
  return content.split(/\n\s*\n/).map((p) => p.trim()).filter(Boolean);
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!));
}

export async function sharePdf(story: Story): Promise<void> {
  const pages = splitPages(story.content);
  const html = `
    <!DOCTYPE html>
    <html lang="${story.language}">
      <head>
        <meta charset="utf-8">
        <title>${escapeHtml(story.title)}</title>
        <style>
          body { font-family: -apple-system, sans-serif; max-width: 600px; margin: 40px auto; padding: 24px; color: #1a1a1a; line-height: 1.6; }
          h1 { font-size: 32px; margin-bottom: 24px; }
          .page { page-break-after: always; margin-bottom: 32px; }
          .page:last-child { page-break-after: auto; }
        </style>
      </head>
      <body>
        <h1>${escapeHtml(story.title)}</h1>
        ${pages.map((p) => `<div class="page">${escapeHtml(p)}</div>`).join('\n')}
      </body>
    </html>
  `;
  const { uri } = await Print.printToFileAsync({ html });
  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(uri, { dialogTitle: story.title, mimeType: 'application/pdf' });
  }
}
