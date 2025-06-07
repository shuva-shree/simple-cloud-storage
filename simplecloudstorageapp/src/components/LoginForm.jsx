import React, { useState } from "react";
import { TextField, Button, Card, Typography } from "@mui/material";
import axios from "axios";
import "../styles/Form.css";
import { useNavigate } from 'react-router-dom';

export default function LoginForm() {
  const [formData, setFormData] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value.trim(),
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(""); // Clear previous error

    try {
      const res = await axios.post("http://localhost:8080/auth/login", {username: formData.username, // Change this line
        password: formData.password}, {
        headers: {
          "Content-Type": "application/json",
        },
      });

      localStorage.setItem("token", res.data.token);
      navigate("/home");
    } catch (err) {
      console.error("Login error:", err.response || err.message);
      setError(err.response?.data?.message || "Login failed. Please try again.");
    }
  };

  return (
    <Card className="form-card">
      <Typography variant="h5" className="form-title">Login to your Cloud Account</Typography>
      <Typography className="welcome-text">Welcome back! Store your files securely in the cloud.</Typography>
      
      {error && <Typography color="error">{error}</Typography>}

      <form onSubmit={handleSubmit} className="form">
        <TextField label="Username" name="username" value={formData.username} onChange={handleChange} fullWidth margin="normal" />
        <TextField label="Password" name="password" type="password" value={formData.password} onChange={handleChange} fullWidth margin="normal" />
        <Button type="submit" variant="contained" color="primary" fullWidth>Login</Button>
      </form>
    </Card>
  );
}
