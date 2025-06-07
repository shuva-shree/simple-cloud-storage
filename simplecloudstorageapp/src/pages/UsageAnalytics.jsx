import React, { useEffect, useState } from "react";
import axios from "axios";
import { Bar, Pie } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend } from 'chart.js';
import NavBar from "../components/NavBar"; // 
import "../styles/NavBar.css";

// Import the CSS file
import '../styles/UsageAnalytics.css'; 

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend);

function UsageAnalytics() {
  const [storageUsage, setStorageUsage] = useState(null);
  const [fileTypeDistribution, setFileTypeDistribution] = useState({});
  const [downloadFrequency, setDownloadFrequency] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchAnalytics = async () => {
      setLoading(true);
      setError(null);
      const token = localStorage.getItem("token");
      if (!token) {
        setError("Authentication token missing. Please log in.");
        setLoading(false);
        return;
      }

      try {
        // Fetch Storage Usage
        const usageRes = await axios.get("http://localhost:8080/api/analytics/storage-usage", {
          headers: { Authorization: `Bearer ${token}` },
        });
        setStorageUsage(usageRes.data.totalStorageBytes);

        // Fetch File Type Distribution
        const typeRes = await axios.get("http://localhost:8080/api/analytics/file-types", {
          headers: { Authorization: `Bearer ${token}` },
        });
        setFileTypeDistribution(typeRes.data);

        // Fetch Download Frequency (Corrected eventType to DOWNLOAD)
        const freqRes = await axios.get("http://localhost:8080/api/analytics/access-frequency?eventType=UPLOAD", {
          headers: { Authorization: `Bearer ${token}` },
        });
        console.log("Download Frequency Data:", freqRes.data);
        setDownloadFrequency(freqRes.data);

      } catch (err) {
        console.error("Error fetching analytics:", err);
        setError("Failed to load analytics data. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    fetchAnalytics();
  }, []);

  const formatBytes = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = 2;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  };

  // Chart data and options
  const fileTypeData = {
    labels: Object.keys(fileTypeDistribution),
    datasets: [
      {
        data: Object.values(fileTypeDistribution),
        backgroundColor: [
          '#42A5F5', // Blue
          '#66BB6A', // Green
          '#FFA726', // Orange
          '#EF5350', // Red
          '#AB47BC', // Purple
          '#78909C', // Grey
          '#26A69A', // Teal
          '#FFCA28', // Amber
        ],
        borderColor: '#ffffff', // White border for slices
        borderWidth: 1,
      },
    ],
  };

  const fileTypeOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: true,
        text: 'Distribution by File Type',
        font: {
          size: 16
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || '';
            const value = context.raw || 0;
            const total = context.dataset.data.reduce((acc, current) => acc + current, 0);
            const percentage = ((value / total) * 100).toFixed(2);
            return `${label}: ${value} files (${percentage}%)`;
          }
        }
      }
    },
  };

  const downloadFreqData = {
    labels: Object.keys(downloadFrequency).sort((a, b) => new Date(a).getTime() - new Date(b).getTime()), // Ensure chronological sorting for dates
    datasets: [
      {
        label: 'Downloads per Day',
        data: Object.keys(downloadFrequency).sort((a, b) => new Date(a).getTime() - new Date(b).getTime()).map(key => downloadFrequency[key]),
        backgroundColor: 'rgba(75, 192, 192, 0.7)',
        borderColor: 'rgba(75, 192, 192, 1)',
        borderWidth: 1,
        barThickness: 'flex', // Adjust bar thickness automatically
        maxBarThickness: 70, // Max width for bars
      },
    ],
  };

  const downloadFreqOptions = {
    responsive: true,
    plugins: {
      legend: {
        display: false, // Often not needed for single-dataset bar charts
      },
      title: {
        display: true,
        text: 'Daily File Download Frequency',
        font: {
          size: 16
        }
      },
    },
    scales: {
      x: {
        title: {
          display: true,
          text: 'Date',
        },
        grid: {
          display: false // Hide x-axis grid lines for cleaner look
        }
      },
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Number of Downloads',
        },
        ticks: {
            stepSize: 1 // Ensure integer ticks for counts
        }
      },
    },
  };


  if (loading) return <div className="loading-message">Loading analytics...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <>
    <NavBar /> 
    <div className="analytics-dashboard">
      <h2 className="dashboard-title">Your Storage Analytics</h2>

      <div className="analytics-grid">
        <div className="analytics-card usage-card">
          <h3>Total Storage Used</h3>
          <p className="storage-value">{formatBytes(storageUsage)}</p>
        </div>

        <div className="analytics-card chart-card">
          <h3>File Type Distribution</h3>
          {Object.keys(fileTypeDistribution).length > 0 ? (
            <div className="chart-container">
              <Pie data={fileTypeData} options={fileTypeOptions} />
            </div>
          ) : (
            <p className="no-data-message">No file type data available.</p>
          )}
        </div>

        <div className="analytics-card chart-card full-width-card">
          <h3>Daily File Download Frequency</h3>
          {Object.keys(downloadFrequency).length > 0 ? (
            <div className="chart-container">
              <Bar data={downloadFreqData} options={downloadFreqOptions} />
            </div>
          ) : (
            <p className="no-data-message">No download frequency data available.</p>
          )}
        </div>
      </div>
    </div>
    </>
  );
}

export default UsageAnalytics;