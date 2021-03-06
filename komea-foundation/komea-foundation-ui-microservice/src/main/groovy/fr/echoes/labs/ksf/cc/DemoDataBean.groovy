/**
 *
 */
package fr.echoes.labs.ksf.cc

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

import com.tocea.corolla.cqrs.gate.Gate
import com.tocea.corolla.products.commands.CreateProjectCommand
import com.tocea.corolla.products.commands.EditProjectCommand
import com.tocea.corolla.products.dao.IProjectDAO
import com.tocea.corolla.products.domain.Project
import com.tocea.corolla.users.commands.CreateRoleCommand
import com.tocea.corolla.users.commands.CreateUserCommand
import com.tocea.corolla.users.commands.CreateUserGroupCommand
import com.tocea.corolla.users.commands.EditRoleCommand
import com.tocea.corolla.users.commands.EditUserCommand
import com.tocea.corolla.users.dao.IRoleDAO
import com.tocea.corolla.users.dao.IUserDAO
import com.tocea.corolla.users.dao.IUserGroupDAO
import com.tocea.corolla.users.domain.Permission
import com.tocea.corolla.users.domain.Role
import com.tocea.corolla.users.domain.User
import com.tocea.corolla.users.domain.UserGroup

import fr.echoes.labs.ksf.cc.sf.dao.ISFApplicationTypeDAO
import fr.echoes.labs.ksf.cc.sf.dao.ISoftwareFactoryDAO
import fr.echoes.labs.ksf.cc.sf.domain.SFApplication
import fr.echoes.labs.ksf.cc.sf.domain.SFApplicationType
import fr.echoes.labs.ksf.cc.sf.domain.SoftwareFactory
import groovy.util.logging.Slf4j

/**
 * @author sleroy
 *
 */
@Profile("demo")
@Component
@Slf4j
public class DemoDataBean {

	static final String ADMIN_USERS = ADMIN_USERS

	@Autowired
	def IRoleDAO					roleDAO

	@Autowired
	def IUserDAO					userDAO

	@Autowired
	def IUserGroupDAO				groupDAO

	@Autowired
	def IProjectDAO					projectDAO

	@Autowired
	def ISFApplicationTypeDAO		applicationTypeDAO

	@Autowired
	def ISoftwareFactoryDAO			softwareFactoryDAO

	@Autowired
	def PasswordEncoder				passwordEncoder

	@Autowired
	def Gate						gate

	@SuppressWarnings("nls")
	@PostConstruct
	public void init() throws MalformedURLException {

		/*
		 * Admin role
		 */
		final Role roleAdmin = this.newRole("Administrator", "Administrator role", Permission.list())
		roleAdmin.roleProtected = true
		this.gate.dispatch new EditRoleCommand(roleAdmin)

		/*
		 * Roles
		 */
		final Role roleGuest = this.newRole("Guest", "Guest", [], true)
		final Role roleTester = this.newRole("Tester", "Tester", [Permission.REST])
		final Role roleTestManager = this.newRole("Test manager", "Test Manager", [Permission.REST])
		final Role roleApplicationManager = this.newRole(	"Application manager",
				"Application manager", [Permission.REST])

		/*
		 * Users
		 */
		User jsnow = this.newUser(	"John", "Snow", "john.snow@email.com", "jsnow",
				"password", roleAdmin)
		def scarreau = this.newUser(	"Sébastien", "Carreau", "sebastien.carreau@tocea.com", "scarreau",
				"scarreau", roleAdmin)
		this.newUser(	"Jack", "Pirate", "jack.pirate@email.com", "jpirate",
				"password", roleGuest)
		this.newUser(	"Ichigo", "Kurosaki", "ichigo.kurosaki@email.com",
				"ikurosaki", "password", roleTester)
		this.newUser(	"James", "Bond", "james.bond@mi6.com", "jbond", "007",
				roleTestManager)
		this.newUser(	"Gandalf", "LeGris", "gandalf.legris@lotr.com",
				"gandalf",
				"saroumaneisg..", roleApplicationManager)
		this.newUser(	"Saroumane", "LeBlanch", "saroumane.leblanc@lotr.com",
				"saroumane",
				"fuckSauron..", roleAdmin)

//		def projet = new Project()
//		projet.setName("test2")
//		projet.setOwnerId(jsnow.getId())
//		projet.setKey("keyTest2")
//		this.saveProject(projet)

        def projet = new Project()
        projet.setName("test")
        projet.setOwnerId(jsnow.getId())
        projet.setKey("keyTest")
        this.saveProject(projet)

//        def projet2 = new Project()
//        projet2.setName("test1")
//        projet2.setOwnerId(jsnow.getId())
//        projet2.setKey("keyTest1")
//        this.saveProject(projet2)


		def projet1 = new Project()
		projet1.setName("test1")
		projet1.setOwnerId(jsnow.getId())
		projet1.setKey("keyTest1")
		this.saveProject(projet1)

		def projet1SubProject1 = new Project()
		projet1SubProject1.setName("test1 subproject 1")
		projet1SubProject1.setOwnerId(jsnow.getId())
		projet1SubProject1.setKey("test1Subproject1")
		projet1SubProject1.setParentId(projet1.getId())

		this.saveProject(projet1SubProject1)

		def projet1SubProject2 = new Project()
		projet1SubProject2.setName("test1 subproject 2")
		projet1SubProject2.setOwnerId(jsnow.getId())
		projet1SubProject2.setKey("test1Subproject2")
		projet1SubProject2.setParentId(projet1.getId())
		this.saveProject(projet1SubProject2)

		def projet1SubProject2SubProject1 = new Project()
		projet1SubProject2SubProject1.setName("test1 subproject 2 subproject 1")
		projet1SubProject2SubProject1.setOwnerId(jsnow.getId())
		projet1SubProject2SubProject1.setKey("test1Subproject2 subproject 1")
		projet1SubProject2SubProject1.setParentId(projet1SubProject2.getId())
		this.saveProject(projet1SubProject2SubProject1)

		def projet1SubProject3 = new Project()
		projet1SubProject3.setName("test1 subproject 3")
		projet1SubProject3.setOwnerId(jsnow.getId())
		projet1SubProject3.setKey("test1Subproject3")
		projet1SubProject3.setParentId(projet1.getId())
		this.saveProject(projet1SubProject3)

		def probe = new Project()
		probe.setName("Probe")
		probe.setOwnerId(jsnow.getId())
		probe.setKey("probe")
		this.saveProject(probe)

		/*
		 * User Groups
		 */
		def developers = this.newGroup("developers", [jsnow, scarreau])


		/*
		 * Software Factory
		 */
		def softwareFactory = new SoftwareFactory(name: "Demo SF");
		softwareFactoryDAO.save(softwareFactory);

		def foreman = new SFApplicationType(name: "foreman");
		applicationTypeDAO.save(foreman)

		def foremanApp = new SFApplication(
			name: "Foreman-1",
			type: foreman,
			softwareFactory: softwareFactory,
			url: "http://"
		);

	}

	/**
	 * Creates a new role.
	 *
	 * @param _roleName
	 *            the role name
	 * @param _roleNote
	 *            the role note
	 * @return the new role
	 */
	Role newRole(final String _roleName, final String _roleNote) {
		final Role role = new Role()
		role.with {
			name = _roleName
			note = _roleNote
			permissions = ""
			roleProtected = false
		}
		this.gate.dispatch new CreateRoleCommand(role)
		log.info("new role created [_id:"+role.getId()+"]")
		return role
	}

	/**
	 * Creates a new role.
	 *
	 * @param _roleName
	 *            the role name
	 * @param _roleNote
	 *            the role note
	 * @return the new role
	 */
	public Role newRole(final String _roleName, final String _roleNote, List<String> _roles, boolean _defaultRole=false) {
		final Role role = new Role()
		role.with {
			name = _roleName
			note = _roleNote
			permissions = _roles.join(", ")
			defaultRole = _defaultRole
			roleProtected = false
		}
		this.gate.dispatch new CreateRoleCommand(role)
		log.info("new role created [_id:"+role.getId()+"]")
		return role
	}

	public User newUser(final String _firstName, final String _lastName,
			final String _email, final String _login,
			final String _password, final Role _rolePO) {
		final User user = new User()
		user.with {
			firstName = _firstName
			lastName = _lastName
			email =_email
			login =_login
			password = _password
			roleId = _rolePO.id
		}
		this.gate.dispatch new CreateUserCommand(user)
		user.active = true
		this.gate.dispatch new EditUserCommand(user)
		log.info("new user created [_id:"+user.getId()+"]")
		return user
	}

	public UserGroup newGroup(final String name, List<User> users) {

		def group = new UserGroup()
		group.setName(name)
		group.setUserIds(users.collect { it.login })

		this.gate.dispatch new CreateUserGroupCommand(group)
		log.info("new user group created [_id: {}", group.getId())

		return group

	}

	public Project saveProject(project) {

		this.gate.dispatch new CreateProjectCommand(project)

		return project

	}

	public Project editProject(project) {

		this.gate.dispatch new EditProjectCommand(project)

		return project
	}



	@PreDestroy
	public void destroy() {

		projectDAO.deleteAll()
		userDAO.deleteAll()
		roleDAO.deleteAll()
		groupDAO.deleteAll()

	}

}