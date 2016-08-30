export default {
  sizePerPage: 20,
  paginationShowsTotal: (start, to, total) => `Showing ${start + 1} to ${to + 1} of ${total + 1} entries`,
  hideSizePerPage: true,
};