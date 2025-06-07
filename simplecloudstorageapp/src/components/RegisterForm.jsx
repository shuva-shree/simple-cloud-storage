import React, { useState } from "react";
import { TextField, Button, Card, Typography } from "@mui/material";
import axios from "axios";
import "../styles/Form.css";
import logo from "../assets/cloud-logo.png"; 
import { Link, useNavigate } from 'react-router-dom';

export default function RegisterForm() {
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: ""
  });

  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await axios.post("http://localhost:8080/auth/register", formData);
      console.log("formData",formData);
      alert("Registration successful");
      navigate("/home");
    } catch (error) {
  if (error.response && error.response.status === 409) {
    alert("Username already exists. Please choose another.");
  } else {
    alert("Registration failed.");
  }
}
  };

  return (
    <Card className="form-card">
      <img src={logo} alt="Logo" className="logo" />
      <Typography className="form-title">Create Your Cloud Account</Typography>
      <Typography className="welcome-text">Welcome! Store your files securely in the cloud.</Typography>

      <form onSubmit={handleSubmit} className="form">
        <TextField label="Username" name="username" value={formData.username} onChange={handleChange} fullWidth />
        <TextField label="Email" name="email" type="email" value={formData.email} onChange={handleChange} fullWidth />
        <TextField label="Password" name="password" type="password" value={formData.password} onChange={handleChange} fullWidth />
        <Button type="submit" variant="contained" fullWidth>Register</Button>
        <div style={{ marginTop: "16px" }}>
      <Typography variant="body2">
        Already have an account?{" "}
        <Link to="/login" style={{ color: "#667eea", fontWeight: "600" }}>
          Log in here
        </Link>
      </Typography>
      </div>
      </form>
      
    </Card>
  );
}
