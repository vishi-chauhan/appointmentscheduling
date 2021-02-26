package org.openmrs.module.appointmentscheduling.rest.resource.openmrs1_9;


import java.util.Date;
import java.util.HashSet;
import java.util.Calendar;

import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.module.appointmentscheduling.rest.controller.AppointmentRestController;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This is a special resource for interacting with AppointmentBlocks in the use case where we want a
 * 1-to-1 relationship with Time Slots (that is, each AppointmentBlock has a single TimeSlot with
 * the same start and end time); this resource overrides the save method of the main
 * AppointmentBlockResource1_9 to create/update an associated Time Slot whenever an AppointmentBlock
 * is updated
 */

@Resource(name = RestConstants.VERSION_1 + AppointmentRestController.APPOINTMENT_SCHEDULING_REST_NAMESPACE + "/recurringappointmentblockwithtimeslot",
    supportedClass = AppointmentBlock.class, supportedOpenmrsVersions = {"1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*", "2.2.*", "2.3.*", "2.4.*"})
public class RecurringAppointmentBlockWithTimeSlot extends AppointmentBlockResource1_9 {

	@Override
	
	// TODO does this actually make this transactional? probably not?
	public AppointmentBlock save(AppointmentBlock appointmentBlock)  {

            Date d1= appointmentBlock.getStartDate();
            Date d2 = appointmentBlock.getEndDate();
            Calendar startdate = Calendar.getInstance();
            startdate.setTime(d1);
            Calendar enddate = Calendar.getInstance(); 
            enddate.setTime(d2);
           if( appointmentBlock.getDaysToSchedule() == null) {
        	   throw new APIException("DaysToSchedule cannot be empty, atleast one day required");
           }
            
            
            Calendar tempdate = Calendar.getInstance();
            int starthour = startdate.get(Calendar.HOUR_OF_DAY);
            int startmin = startdate.get(Calendar.MINUTE);
            
            int endhour = enddate.get(Calendar.HOUR_OF_DAY);
            int endmin = enddate.get(Calendar.MINUTE);
            
            while(enddate.compareTo(startdate)>=0){
            	if(appointmentBlock.getDaysToSchedule().contains(startdate.get(Calendar.DAY_OF_WEEK))) {
            	AppointmentBlock app = new AppointmentBlock();
            	startdate.set(Calendar.HOUR_OF_DAY, starthour);
                startdate.set(Calendar.MINUTE, startmin);
                d1 = startdate.getTime();
                
            	app.setLocation(appointmentBlock.getLocation());
            	app.setProvider(appointmentBlock.getProvider());
            	app.setTypes(appointmentBlock.getTypes());
            	app.setStartDate(d1);
            	
                tempdate = startdate;
                tempdate.set(Calendar.HOUR_OF_DAY, endhour);
                tempdate.set(Calendar.MINUTE, endmin);
                
                d2 = tempdate.getTime();
                app.setEndDate(d2);
                AppointmentBlock appointments =Context.getService(AppointmentService.class).saveAppointmentBlock(app);
                multisave(appointments);
            	}
                startdate.add(Calendar.DATE, 1);
                Context.flushSession();
            
            }
		

		return appointmentBlock;
	}
	
	
	private AppointmentBlock multisave(AppointmentBlock appointmentBlock) {

      
        appointmentBlock = Context.getService(AppointmentService.class).saveAppointmentBlock(appointmentBlock);
	List<TimeSlot> timeSlots = Context.getService(AppointmentService.class).getTimeSlotsInAppointmentBlock(
	    appointmentBlock);
	if (timeSlots == null || timeSlots.isEmpty()) {
		createTimeSlotInAppointmentBlock(appointmentBlock);
	} else {
		updateTimeSlotInAppointmentBlock(appointmentBlock, timeSlots.get(0));
		if (timeSlots.size() > 1) {
			voidTimeSlots(timeSlots.subList(1, timeSlots.size()));
		}
	}
        
        
	

	return appointmentBlock;
}

	private TimeSlot createTimeSlotInAppointmentBlock(AppointmentBlock appointmentBlock) {

		TimeSlot timeSlot = new TimeSlot();
		timeSlot.setAppointmentBlock(appointmentBlock);
		timeSlot.setStartDate(appointmentBlock.getStartDate());
		timeSlot.setEndDate(appointmentBlock.getEndDate());

		return Context.getService(AppointmentService.class).saveTimeSlot(timeSlot);
	}

	private TimeSlot updateTimeSlotInAppointmentBlock(AppointmentBlock appointmentBlock, TimeSlot timeSlot) {

		timeSlot.setStartDate(appointmentBlock.getStartDate());
		timeSlot.setEndDate(appointmentBlock.getEndDate());

		return Context.getService(AppointmentService.class).saveTimeSlot(timeSlot);
	}

	private void voidTimeSlots(List<TimeSlot> timeSlots) {

		for (TimeSlot timeSlot : timeSlots) {
			Context.getService(AppointmentService.class).voidTimeSlot(timeSlot,
			    "appointment block has more than one time slot");
		}

	}

}
