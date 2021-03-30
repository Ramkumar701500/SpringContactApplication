package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.service.EmailService;

@Controller
public class ForgotController 
{
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	Random randomNumber = new Random(1000);
	
	// Forgot page
	@GetMapping("/forgot")
	public String emailForm(Model m)
	{
		
		m.addAttribute("title", "Forgot Password");
		return "forgot-email-form";
	}
	
	// OTP Page
	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam("email") String email, HttpSession session)
	{
		
		User user = this.userRepository.getUserByUsername(email);
		
		if(user==null)
		{
			session.setAttribute("message", new Message("Your Email Is Not Registered !!!", "danger"));
			return "redirect:/forgot";
		}
		
		else
		{
		
			int emailOtp = randomNumber.nextInt(9999);
			System.out.println("OTP "+emailOtp);
			System.out.println("Email "+email);
			
			String to = email;
			String subject = "OTP From Smart Contact Application";
			String message = "<div style='border:1px solid #e2e2e2; background:lightblue; padding:20px'>"
							+ "<h1>"+"Your OTP is: "+emailOtp+"</h1>"
							+ "</div>";
			
			
			boolean flag = this.emailService.sendEmail(subject, message, to);
			
			// If OTP successfully sent to given email
			if(flag==true)
			{
				session.setAttribute("emailOtp", emailOtp);
				session.setAttribute("email", email);
				
				session.setAttribute("message", new Message("We Have Sent An OTP To Your Email !!!", "success"));
				return "verify-otp";
			}
			else
			{
				session.setAttribute("message", new Message("Enter Correct Email Address !!!", "danger"));
				return "redirect:/forgot";
			}
		}
		
	}
	
	
	@PostMapping("/verify-otp-handler")
	public String verifyOtpController(@RequestParam("userOtp") int userOtp, HttpSession session)
	{
		int emailOtp = (int)session.getAttribute("emailOtp");
		String email = (String)session.getAttribute("email");
		
		// If OTP entered by user equals to  OTP sent to given email
		if(userOtp == emailOtp)
		{
			session.setAttribute("email", email);
			return "change-password";
		}
		
		else
		{
			session.setAttribute("message", new Message("You Have Entered Wrong OTP !!!", "danger"));
			return "verify-otp";
		}	
	}
	
	
	// Change password handler
	@PostMapping("/change-password-controller")
	public String changePassword(@RequestParam("newPassword") String newPassword, HttpSession session)
	{
		String email = (String)session.getAttribute("email");		
		User currentUser = userRepository.getUserByUsername(email);

		currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
		this.userRepository.save(currentUser);
			
		session.setAttribute("message", new Message("Password Changed Succssfully. You Can Login Now !!!", "alert-success"));
		System.out.println("Password Changed With New Password "+newPassword);

		return "redirect:/login";
	}
	
	
	
	
	
	
}
