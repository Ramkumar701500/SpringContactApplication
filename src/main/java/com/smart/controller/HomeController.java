package com.smart.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
public class HomeController 
{
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	
	@RequestMapping("/")
	public String home(Model model) 
	{
		model.addAttribute("title", "Home - Smart Contact Application");
		return "home";
	}
	
	
	@RequestMapping("/features")
	public String about(Model model) 
	{
		model.addAttribute("title", "About - Smart Contact Application");
		return "features";
	}
	
	
	@RequestMapping("/register")
	public String register(Model model)
	{
		model.addAttribute("title", "Register - Smart Contact Application");
		model.addAttribute("user", new User());
		return "register";
	}
	
	
	@PostMapping("/register-user")
	public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result, 
			@RequestParam(value="agreement", defaultValue = "false") boolean agreement, 
			Model model, HttpSession session)
	{
	
		// Checking registering user email is present in database or not, if not then it will be null
		User registeringUser = userRepository.getUserByUsername(user.getEmail());
		
		try
		{
			if(!agreement)
			{
				System.out.println("You have not agreed to terms and conditions !!!");
				throw new Exception("You have not agreed to terms and conditions !!!");
			}
			
			if(registeringUser != null)
			{
				System.out.println("You have not agreed to terms and conditions !!!");
				throw new Exception("Email Is Already Registered !!!");
			}
			
			if(result.hasErrors())
			{
				System.out.println("Error "+result.toString());
				model.addAttribute("user", user);
				return "register";
			}
			
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("defaul.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			
			System.out.println("Agreement "+agreement);
			System.out.println("User "+user);
			
			User r  = userRepository.save(user);
			
			model.addAttribute("user",new User());
			session.setAttribute("message", new Message("Registered Successfully. You Can Login Now !!!", "alert-success"));
			
			return "redirect:/login";
		}
		
		catch(Exception e)
		{
			model.addAttribute("user", user);
			session.setAttribute("message", new Message(e.getMessage(), "alert-danger"));
			e.printStackTrace();
			
			return "register";
		}		
	}
	
	
	@RequestMapping("/login")
	public String login(Model model)
	{
		model.addAttribute("title", "Login - Smart Contact Application");
		return "login";
	}
	
	

}
