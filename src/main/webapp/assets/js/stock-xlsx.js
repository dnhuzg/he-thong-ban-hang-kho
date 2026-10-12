/* GHTK-83: tạo tệp XLSX tối giản phía trình duyệt, không cần thư viện ngoài. */
(() => {
  "use strict";
  const encoder = new TextEncoder();
  const xml = (value) => String(value).replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&apos;" })[c]);
  const col = (n) => { let s = ""; for (n++; n; n = Math.floor((n - 1) / 26)) s = String.fromCharCode(65 + (n - 1) % 26) + s; return s; };
  const crcTable = Array.from({ length: 256 }, (_, i) => { let c = i; for (let j = 0; j < 8; j++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  function crc32(bytes) { let c = 0xffffffff; for (const b of bytes) c = crcTable[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; }
  function u16(view, offset, value) { view.setUint16(offset, value, true); }
  function u32(view, offset, value) { view.setUint32(offset, value, true); }
  function join(chunks) { const output = new Uint8Array(chunks.reduce((n, x) => n + x.length, 0)); let at = 0; for (const chunk of chunks) { output.set(chunk, at); at += chunk.length; } return output; }
  function zip(files) {
    const parts = [], central = [];
    let position = 0;
    for (const [filename, content] of files) {
      const name = encoder.encode(filename), data = encoder.encode(content), crc = crc32(data);
      const local = new Uint8Array(30 + name.length), lv = new DataView(local.buffer);
      u32(lv, 0, 0x04034b50); u16(lv, 4, 20); u16(lv, 6, 0x800); u32(lv, 14, crc); u32(lv, 18, data.length); u32(lv, 22, data.length); u16(lv, 26, name.length); local.set(name, 30);
      const record = new Uint8Array(46 + name.length), cv = new DataView(record.buffer);
      u32(cv, 0, 0x02014b50); u16(cv, 4, 20); u16(cv, 6, 20); u16(cv, 8, 0x800); u32(cv, 16, crc); u32(cv, 20, data.length); u32(cv, 24, data.length); u16(cv, 28, name.length); u32(cv, 42, position); record.set(name, 46);
      parts.push(local, data); central.push(record); position += local.length + data.length;
    }
    const center = join(central), end = new Uint8Array(22), ev = new DataView(end.buffer);
    u32(ev, 0, 0x06054b50); u16(ev, 8, files.length); u16(ev, 10, files.length); u32(ev, 12, center.length); u32(ev, 16, position);
    return join([...parts, center, end]);
  }
  function build(rows) {
    const sheetRows = rows.map((row, i) => `<row r="${i + 1}">${row.map((value, j) => {
      const cell = `${col(j)}${i + 1}`;
      if (typeof value === "number" && Number.isFinite(value)) return `<c r="${cell}"><v>${value}</v></c>`;
      return `<c r="${cell}" t="inlineStr"><is><t xml:space="preserve">${xml(value ?? "")}</t></is></c>`;
    }).join("")}</row>`).join("");
    const sheet = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${sheetRows}</sheetData></worksheet>`;
    const files = [
      ["[Content_Types].xml", `<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>`],
      ["_rels/.rels", `<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>`],
      ["xl/workbook.xml", `<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Ton kho" sheetId="1" r:id="rId1"/></sheets></workbook>`],
      ["xl/_rels/workbook.xml.rels", `<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>`],
      ["xl/worksheets/sheet1.xml", sheet]
    ];
    return zip(files);
  }
  window.StockXlsx = { build };
})();
