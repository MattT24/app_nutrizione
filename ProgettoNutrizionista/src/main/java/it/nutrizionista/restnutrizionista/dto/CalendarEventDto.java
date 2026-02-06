package it.nutrizionista.restnutrizionista.dto;


import java.time.LocalDateTime;
import java.util.Map;

public class CalendarEventDto {
    private Long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean allDay = false;

    // tutto ciò che vuoi in FullCalendar extendedProps
    private Map<String, Object> extendedProps;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public void setEnd(LocalDateTime end) {
		this.end = end;
	}

	public boolean isAllDay() {
		return allDay;
	}

	public void setAllDay(boolean allDay) {
		this.allDay = allDay;
	}

	public Map<String, Object> getExtendedProps() {
		return extendedProps;
	}

	public void setExtendedProps(Map<String, Object> extendedProps) {
		this.extendedProps = extendedProps;
	}

   
}
