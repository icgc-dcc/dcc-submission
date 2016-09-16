import React from 'react';

function Calendar() {
  return (
    <div className="container">
      <iframe src="https://www.google.com/calendar/embed?src=icgcportal%40gmail.com&ctz=America/Toronto" style={{border: 0}} width="100%" height={300} frameBorder={0} scrolling="no" />
    </div>
  );
}

export default Calendar;