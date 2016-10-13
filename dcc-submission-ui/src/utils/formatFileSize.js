export function formatFileSize(fs) {
  var bytes, posttxt, precision, sizes;
  sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  posttxt = 0;
  bytes = fs * 1;
  precision = 2;
  if (bytes <= 1024) {
    precision = 0;
  }
  while (bytes >= 1024) {
    posttxt++;
    bytes /= 1024;
  }
  return Number(bytes).toFixed(precision) + " " + sizes[posttxt];
}