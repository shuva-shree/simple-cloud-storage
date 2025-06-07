import React, { useEffect, useState } from "react";
import axios from "axios";
import "../styles/Home.css"; // Ensure this import is correct
import { FiClock, FiBarChart2 } from "react-icons/fi";
import SearchBar from "../components/SearchBar";
import UploadSection from "../components/UploadSection";
import FileCard from "../components/FileCard";
import { Link, useNavigate } from 'react-router-dom';
import NavBar from "../components/NavBar"; // 
import "../styles/NavBar.css";

function formatFileSize(bytes) {
  if (bytes == null || isNaN(bytes)) return "Unknown size";
  if (bytes === 0) return "0 Bytes";

  const units = ["Bytes", "KB", "MB", "GB", "TB"];
  const exp = Math.floor(Math.log(bytes) / Math.log(1024));
  const size = (bytes / Math.pow(1024, exp)).toFixed(exp > 0 ? 1 : 0);

  return `${size} ${units[exp]}`;
}

// Enhanced date formatter (moved outside component for consistency and reusability)
const formatVersionDate = (dateInput) => {
  if (!dateInput) return "Unknown date";

  try {
    // Handle multiple possible date formats from backend
    const date = new Date(dateInput);
    return isNaN(date.getTime())
      ? "Invalid date"
      : date.toLocaleString("en-US", {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          });
  } catch (e) {
    console.error("Error formatting date:", dateInput, e);
    return "Invalid date";
  }
};


function HomePage() {
  const [files, setFiles] = useState([]);
  const [filteredFiles, setFilteredFiles] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [replaceFileId, setReplaceFileId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [isPublic, setIsPublic] = useState(false);
  const [selectedTag, setSelectedTag] = useState("");
  const [tags, setTags] = useState([]);
  const [versionsModalOpen, setVersionsModalOpen] = useState(false);
  const [selectedFileVersions, setSelectedFileVersions] = useState([]);
  const [currentFileForVersions, setCurrentFileForVersions] = useState(null);

  const fileInputRef = React.useRef();

  // Log component mount
  useEffect(() => {
    console.log("HomePage mounted");
    fetchFiles();
  }, []);

  // Log state changes for key states
  useEffect(() => {
    console.log("HomePage: Files state updated", files.length);
  }, [files]);

  useEffect(() => {
    console.log("HomePage: Filtered Files state updated", filteredFiles.length);
  }, [filteredFiles]);

  useEffect(() => {
    console.log("HomePage: selectedFileVersions state updated", selectedFileVersions);
  }, [selectedFileVersions]);

  useEffect(() => {
    console.log("HomePage: versionsModalOpen state updated", versionsModalOpen);
  }, [versionsModalOpen]);


  const fetchFiles = async () => {
    console.log("Fetching files...");
    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        setError("No authentication token found. Please log in.");
        setLoading(false);
        return;
      }
      const res = await axios.get("http://localhost:8080/api/files", {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log("Files fetched successfully:", res.data);
      setFiles(res.data);
      setFilteredFiles(res.data);
      extractTags(res.data);
      setError(null);
    } catch (err) {
      console.log("token",localStorage.getItem("token"));
      console.error("Error fetching files:", err.response?.data || err.message);
      setError("Failed to load files. Please log in again.");
    } finally {
      setLoading(false);
      console.log("Finished fetching files.");
    }
  };

  const extractTags = (filesData) => {
    console.log("Extracting tags from files:", filesData);
    const allTags = new Set();
    filesData.forEach((file) => {
      if (file.tags) {
        file.tags.forEach((tag) => allTags.add(tag));
      }
    });
    const uniqueTags = Array.from(allTags);
    setTags(uniqueTags);
    console.log("Extracted unique tags:", uniqueTags);
  };

  const searchFiles = async () => {
    console.log(`Searching files with query: "${searchQuery}" and tag: "${selectedTag}"`);
    try {
      const token = localStorage.getItem("token");
      const res = await axios.get(
        `http://localhost:8080/api/files/search?query=${searchQuery}${
          selectedTag ? `&tag=${selectedTag}` : ""
        }`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      console.log("Search results:", res.data);
      setFilteredFiles(res.data);
    } catch (err) {
      console.error("Search failed:", err.response?.data || err.message);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    console.log("Search button clicked or form submitted.");
    searchFiles();
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      console.log("No file selected for upload.");
      return;
    }

    console.log("Initiating file upload:", selectedFile.name);
    const formData = new FormData();
    formData.append("file", selectedFile);
    formData.append("isPublic", isPublic);

    try {
      const token = localStorage.getItem("token");
      await axios.post("http://localhost:8080/api/files/upload", formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      });
      console.log("File uploaded successfully.");
      setSelectedFile(null);
      fetchFiles();
    } catch (err) {
      console.error("Upload failed:", err.response?.data || err.message);
      setError("Failed to upload file");
    }
  };

  const handleDownload = async (fileId) => {
    console.log("Attempting to download file with ID:", fileId);
    try {
      const token = localStorage.getItem("token");

      const response = await axios.get(
        `http://localhost:8080/api/files/download/${fileId}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
          responseType: "blob",
        }
      );

      console.log("Download response received. Content-Type:", response.headers["content-type"]);

      const blob = new Blob([response.data], {
        type: response.headers["content-type"],
      });
      console.log("Created Blob MIME type:", blob.type);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;

      const contentDisposition = response.headers["content-disposition"];
      let filename = "file";
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?(.+)"?/);
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1];
          console.log("Extracted filename from headers:", filename);
        } else {
          console.warn("Filename not found in Content-Disposition header.");
        }
      } else {
        console.warn("Content-Disposition header not found in download response.");
      }

      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();

      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      console.log("File download initiated and cleaned up.");
    } catch (err) {
      console.error("Download failed:", err.response?.data || err.message);
      setError("Failed to download file. Please try again.");
    }
  };

  const handleDelete = async (fileId) => {
    console.log("Attempting to delete file with ID:", fileId);
    try {
      const token = localStorage.getItem("token");
      await axios.delete(`http://localhost:8080/api/files/delete/${fileId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log("File deleted successfully:", fileId);
      fetchFiles();
    } catch (err) {
      console.error("Delete failed:", err.response?.data || err.message);
      setError("Failed to delete file");
    }
  };

  const handleReplace = async () => {
    if (!selectedFile || !replaceFileId) {
      console.log("Cannot replace: No file selected or no replaceFileId set.");
      return;
    }

    console.log(`Initiating file replacement for ID: ${replaceFileId} with file: ${selectedFile.name}`);
    const formData = new FormData();
    formData.append("file", selectedFile);

    try {
      const token = localStorage.getItem("token");
      await axios.put(
        `http://localhost:8080/api/files/${replaceFileId}/update`,
        formData,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );
      console.log(`File ID ${replaceFileId} replaced successfully.`);
      setSelectedFile(null);
      setReplaceFileId(null);
      fetchFiles();
    } catch (err) {
      let errorMessage = "Failed to replace file";
      if (err.response) {
        if (err.response.status === 413) {
          errorMessage = "File is too large (max 50MB)";
        } else {
          errorMessage = err.response.data.message || errorMessage;
        }
      }
      console.error("Replace failed:", err.response?.data || err.message);
      setError(errorMessage);
    }
  };

  const handleViewVersions = async (fileId) => {
    console.log("Attempting to view versions for file ID:", fileId);
    setCurrentFileForVersions(fileId);
    try {
      const token = localStorage.getItem("token");
      const res = await axios.get(
        `http://localhost:8080/api/files/${fileId}/versions`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        }
      );

      console.log("Raw versions data received:", res.data);

      // Process versions data before setting state
      const processedVersions = res.data.map((version) => {
        

        const uniqueKey = version.versionId || version.originalId || `${fileId}-${version.lastModified}`;
        console.log(`Processing version: ${uniqueKey}, lastModified: ${version.lastModified}, size: ${version.size},version:${version.version}`);
        return {
          ...version,
          formattedDate: formatVersionDate(version.lastModified),
          formattedSize: formatFileSize(version.size),
          isRestorable: version.versionId !== res.data[0]?.versionId,
          key: uniqueKey,
        };
      });

      setSelectedFileVersions(processedVersions);
      setVersionsModalOpen(true);
      console.log("Processed versions for modal:", processedVersions);
    } catch (err) {
      console.error("Failed to get versions:", err.response?.data || err.message);
      setError(err.response?.data?.message || "Failed to load file versions");
    }
  };


  const handleRestoreVersion = async (versionId) => {
    console.log(`Attempting to restore version ID: ${versionId} for file ID: ${currentFileForVersions}`);
    try {
      const token = localStorage.getItem("token");
      await axios.post(
        `http://localhost:8080/api/files/${currentFileForVersions}/versions/${versionId}/restore`,
        {},
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      console.log(`Version ${versionId} restored successfully.`);
      setVersionsModalOpen(false);
      fetchFiles();
      alert("Version restored successfully!");
    } catch (err) {
      console.error("Failed to restore version:", err.response?.data || err.message);
      setError("Failed to restore version");
    }
  };

  return (
    <> 
      <NavBar /> 
      <div className="cloud-container">
        <div className="cloud-header">
          <h1 className="cloud-title">Your Cloud Files</h1>
          {/* New container for actions */}
          <div className="header-actions">
            <SearchBar
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              selectedTag={selectedTag}
              setSelectedTag={setSelectedTag}
              tags={tags}
              handleSearch={handleSearch}
            />
            <Link to="/analytics" className="analytics-button">
              <FiBarChart2 className="analytics-icon" />
              <span>Usage Analytics</span>
            </Link>
          </div>
        </div>

        {loading && (
          <div className="loading-overlay">
            <div className="spinner" />
            <p>Loading your files...</p>
          </div>
        )}

        {error && (
          <div className="error-banner">
            <p>{error}</p>
            <button onClick={() => setError(null)}>×</button>
          </div>
        )}

        <UploadSection
          selectedFile={selectedFile}
          setSelectedFile={setSelectedFile}
          isPublic={isPublic}
          setIsPublic={setIsPublic}
          handleUpload={handleUpload}
        />

        {replaceFileId && (
          <div className="replace-modal">
            <div className="modal-content">
              <h3>Replace File</h3>
              <p>Select a new file to replace the existing one</p>

              <input
                type="file"
                ref={fileInputRef}
                onChange={(e) => {
                  setSelectedFile(e.target.files[0]);
                  console.log("File selected for replacement:", e.target.files[0]?.name);
                }}
              />

              <div className="modal-actions">
                <button onClick={() => {
                  setReplaceFileId(null);
                  setSelectedFile(null); // Clear selected file when cancelling
                  console.log("Replacement modal cancelled.");
                }}>Cancel</button>
                <button onClick={handleReplace} disabled={!selectedFile}>
                  Confirm Replace
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="file-grid">
          {filteredFiles.length > 0 ? (
            filteredFiles.map((file) => (
              <FileCard
                key={file.fileId}
                file={file}
                handleDownload={handleDownload}
                handleDelete={handleDelete}
                handleViewVersions={handleViewVersions}
                setReplaceFileId={setReplaceFileId}
                formatFileSize={formatFileSize}
              />
            ))
          ) : (
            !loading && (
              <div className="empty-state">No files found. Upload your first file!</div>
            )
          )}
        </div>

        {versionsModalOpen && (
          <div className="versions-modal">
            <div className="modal-content">
              <h3>File Versions </h3>
              <div className="versions-list">
                {selectedFileVersions.length > 0 ? (
                  selectedFileVersions.map((version, index) => (
                    <div
                      key={version.key || version.versionId || index}
                      className="version-item"
                    >
                      <div className="version-info">
                        <span className="version-date">
                          <FiClock /> {`V${index + 1}`}
                        </span>
                        <span className="version-size">{version.formattedSize}</span>
                      </div>
                      <button
                        onClick={() => handleRestoreVersion(version.versionId)}
                        className="restore-button"
                        disabled={!version.isRestorable}
                        title={
                          version.isRestorable
                            ? "Restore this version"
                            : "Cannot restore current version"
                        }
                      >
                        Restore
                      </button>
                    </div>
                  ))
                ) : (
                  <p>No previous versions found for this file. Try replacing the file to create new versions.</p>
                )}
              </div>
              <div className="modal-actions">
                <button onClick={() => {
                  setVersionsModalOpen(false);
                  setSelectedFileVersions([]);
                  setCurrentFileForVersions(null);
                  console.log("Versions modal closed.");
                }}>Close</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
}

export default HomePage;