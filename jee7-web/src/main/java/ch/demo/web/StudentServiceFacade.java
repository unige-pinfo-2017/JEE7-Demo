/**
 * 
 */
package ch.demo.web;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.slf4j.Logger;

import ch.demo.business.service.StudentService;
import ch.demo.dom.Address;
import ch.demo.dom.PhoneNumber;
import ch.demo.dom.Student;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

/**
 * @author hostettler
 * 
 */
@Path("/studentService")
@Stateless
@LocalBean
public class StudentServiceFacade implements Serializable {

	private static final int GRADE_DISTRIBUTION = 10;

	private static final long serialVersionUID = 1318211294294344900L;

	
	@EJB
	private StudentService studentService;

	@Inject
	private Logger logger;
	
	@Inject
	private StudentEventBus bus;
	
	private MapperFacade mapper;
	
	@Resource
	SessionContext ctx;

	@Resource
	private Principal principal;
	
	public StudentServiceFacade() {
		MapperFactory factory = new DefaultMapperFactory.Builder().build();
		factory.registerClassMap(factory.classMap(Student.class,StudentDTO.class).
				field("id", "id")
				.byDefault().toClassMap());
		this.mapper = factory.getMapperFacade();
	}
	
	@GET
	@Produces({ "application/json" })
	public List<StudentDTO> getStudents() {
		logger.info("Fetch all students");
		List<Student> students = studentService.getAll();		
		return mapper.mapAsList(students, StudentDTO.class); 
	}
	
	@GET
	@Path("{id}")
	@Produces({ "application/json" })
	public StudentDTO getStudent(@PathParam("id") Long id) {
		logger.info("Fetch a given student");
		Student student = studentService.getStudentById(id);
		return mapper.map(student, StudentDTO.class);
	}
	
	@POST
	@Consumes({ "application/json" })
	public void save(@Valid StudentDTO studentDTO) throws IOException {
		Student student = mapper.map(studentDTO, Student.class);
		logger.info("save a given student");
		studentService.update(student);
		bus.alertOtherPeers(ctx.getCallerPrincipal(), studentDTO);
	}
	
	@PUT
	@Consumes({ "application/json" })
	public void create(@Valid StudentDTO studentDTO) {
		logger.info("create a given student");
		Student student = mapper.map(studentDTO, Student.class);
		student.setAddress(new Address());
		student.setPhoneNumber(new PhoneNumber(33, 3, 3333));
		studentService.add(student);

	}

	@GET
	@Produces({ "application/xml", "application/json" })
	@Path("distribution")
	public Integer[] getDistribution() {
		logger.info("Compute a distribution over {0} positions", GRADE_DISTRIBUTION);
		Integer[] distrib = studentService.getDistribution(GRADE_DISTRIBUTION);
		return distrib;
	}
}