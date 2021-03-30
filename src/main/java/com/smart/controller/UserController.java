package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController 
{
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	// This function will be called automatically for all the handlers.
	// User data will be saved in Model class object automatically.
	@ModelAttribute
	public void addCommonData(Model model, Principal principal)
	{
		String username = principal.getName();
		// get the User using the Username(Email).
		User user = userRepository.getUserByUsername(username);
		model.addAttribute("user", user);
	}
	
	// User Dashboard Page
	@RequestMapping("/dashboard")
	public String dashboard(Model model, Principal principal)
	{
		model.addAttribute("title", "Dashboard");
		return "user/dashboard";
	}
	
	// Add Contact Page
	@GetMapping("/add-contact")
	public String addContact(Model model)
	{
		model.addAttribute("title", "Add Contacts");
		model.addAttribute("contact", new Contact());
		return "user/add-contact";
	}
	
	// Submit and Process Contact
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session)
	{
		try
		{
			String username = principal.getName();
			User user = userRepository.getUserByUsername(username);
			
			if(file.isEmpty())
			{
				System.out.println("Image File Is Empty !!!");
			
				contact.setImage(user.getId()+"_"+contact.getPhone()+"_default-photo.png"); 	

				File saveFile = new ClassPathResource("static/contact-image").getFile();

				Path defaultImagePath = Paths.get(saveFile.getAbsolutePath() + File.separator + "default-photo.png");
				
				Path imagePath = Paths.get(saveFile.getAbsolutePath() + File.separator + user.getId()
								+"_"+contact.getPhone()+"_default-photo.png");

				Files.copy(defaultImagePath, imagePath, StandardCopyOption.REPLACE_EXISTING);
			}
			
			else
			{
				contact.setImage(user.getId()+"_"+contact.getPhone()+"_" +file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/contact-image").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + user.getId()
				+"_"+contact.getPhone()+"_"+file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Image file is uploaded");			
			}
			
			user.getContacts().add(contact);
			contact.setUser(user);
			userRepository.save(user);
			
			System.out.println("Contact Added to data base");
			System.out.println("Contact " +contact);
			
			// Success Message	
			session.setAttribute("message", new Message("Contact Saved Successfully !!!", "success"));
		}
		
		catch(Exception e)
		{
			System.out.println("Error" +e.getMessage());
			e.printStackTrace();
			
			// Error Message	
			session.setAttribute("message", new Message("Something Went Wrong, Try Again !!!", "danger"));
		}
		
		return "redirect:/user/add-contact";
	}
	
	
	// Show All Contacts Page
	@GetMapping("/view-contact/{page}")
	public String viewContact(@PathVariable("page") Integer page, Model m, Principal principal)
	{
		
		String username = principal.getName();
		
		User user = this.userRepository.getUserByUsername(username);
		
		Pageable pageable = PageRequest.of(page, 5);
		
		Page<Contact> contacts = this.contactRepository.findContactsByUserId(user.getId(), pageable);
		
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		
		
		m.addAttribute("title", "View Contacts");
		return "user/view-contact";
	}
	
	// Contact Detail Page
	@RequestMapping("/contact/{cId}")
	public String contactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal)
	{
		
		Contact contact = this.contactRepository.findById(cId).get();
		
		String username = principal.getName();
		User user = this.userRepository.getUserByUsername(username);
		
		if(user.getId() == contact.getUser().getId())
		{
			model.addAttribute("contact", contact);
			model.addAttribute("title", "Contact - "+contact.getName());
		}
	
		return "user/contact-detail";
	}
	
	// Delete Contact
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, HttpSession session)
	{
		
		Contact contact = this.contactRepository.findById(cId).get();
		contact.setUser(null);
		this.contactRepository.delete(contact);
		
		System.out.println("Contact Deleted");
		
		session.setAttribute("message", new Message("Contact Deleted Succssfully !!!", "success"));
		
		return "redirect:/user/view-contact/0";
	}
	

	// Update Contact Page
	@PostMapping("/update-contact/{cid}")
	public String updateContact(@PathVariable("cid") Integer cId, Model m)
	{	
		Contact contact = this.contactRepository.findById(cId).get();
		
		m.addAttribute("contact", contact);
		m.addAttribute("title", "Update Contact");
		
		return "user/update-contact";
	}
	
	// Update and Process Contact
	@PostMapping("/process-update")
	public String processUpdate(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal, Model m, HttpSession session)
	{
		// Getting old contact detail
		Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
		
		// Getting current user
		String username = principal.getName();
		User user = this.userRepository.getUserByUsername(username);
		
		try
		{
			// If Old image is still present i.e. not updated	
			if(file.isEmpty())
			{		
				contact.setImage(oldContactDetail.getImage());
			}
			
			else
			{
				// Update new image
				contact.setImage(user.getId()+"_"+contact.getPhone()+"_" +file.getOriginalFilename()); 	

				File saveFile = new ClassPathResource("static/contact-image").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + user.getId()+"_"
				+contact.getPhone()+"_" +file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("New Image file is uploaded");	
				
				
				// Delete old image
				File deleteFile = new ClassPathResource("static/contact-image").getFile();
				
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				
				file1.delete();	
				
			}
			
			// Update contact informations
			contact.setUser(user);
			this.contactRepository.save(contact);
			
			// Success Message
			session.setAttribute("message", new Message("Contact Updated Successfully !!!", "success"));
			System.out.println("Contacted Updated");
			
		}
		
		catch(Exception e)
		{
			System.out.println("Error" +e.getMessage());
			e.printStackTrace();
			
			// Error Message	
			session.setAttribute("message", new Message("Something Went Wrong, Try Again !!!", "danger"));
		}
		
		return "redirect:/user/contact/"+contact.getcId();
	}
		
	// Show profile
	@GetMapping("/profile")
	public String profile(Model model)
	{
		// Current user is already added to model by addCommonData function
		model.addAttribute("title", "My Profile");
		
		return "user/profile";
	}
	
	// Settings page
	@GetMapping("/settings")
	public String settings(Model model)
	{
		
		model.addAttribute("title", "Settings");
		
		return "user/settings";
	}
	
	// Change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession session)
	{
		String username = principal.getName();
		User currentUser = userRepository.getUserByUsername(username);
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
			// Change the password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			
			session.setAttribute("message", new Message("Your Password Changed Succssfully !!!", "success"));
		}
		else
		{
			// Print error message
			session.setAttribute("message", new Message("Wrong Old Password !!!", "danger"));
		}
		
		
		System.out.println("current user "+currentUser);
		System.out.println("Old Password "+oldPassword);
		System.out.println("New Password "+newPassword);
		return "redirect:/user/settings";
	}
	
	
}
