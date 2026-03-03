import React from 'react';
import { Mail, Phone, MapPin, Send } from 'lucide-react';

export default function Contact() {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Handle form submission
  };

  return (
    <div className="max-w-6xl mx-auto px-4 space-y-16">
      {/* Contact Info */}
      <section className="grid md:grid-cols-3 gap-8">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="bg-blue-50 p-3 rounded-full w-fit mb-4">
            <Mail className="h-6 w-6 text-blue-600" />
          </div>
          <h3 className="font-semibold text-lg mb-2">Email Us</h3>
          <p className="text-gray-600">support@hospitalconnect.com</p>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="bg-blue-50 p-3 rounded-full w-fit mb-4">
            <Phone className="h-6 w-6 text-blue-600" />
          </div>
          <h3 className="font-semibold text-lg mb-2">Call Us</h3>
          <p className="text-gray-600">+1 (555) 123-4567</p>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="bg-blue-50 p-3 rounded-full w-fit mb-4">
            <MapPin className="h-6 w-6 text-blue-600" />
          </div>
          <h3 className="font-semibold text-lg mb-2">Visit Us</h3>
          <p className="text-gray-600">123 Healthcare Ave, Medical District, CA 90210</p>
        </div>
      </section>

      {/* Contact Form */}
      <section className="bg-white rounded-xl shadow-sm border border-gray-100 p-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Send us a Message</h2>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                Full Name
              </label>
              <input
                type="text"
                id="name"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                Email Address
              </label>
              <input
                type="email"
                id="email"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>
          </div>
          <div>
            <label htmlFor="subject" className="block text-sm font-medium text-gray-700 mb-1">
              Subject
            </label>
            <input
              type="text"
              id="subject"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            />
          </div>
          <div>
            <label htmlFor="message" className="block text-sm font-medium text-gray-700 mb-1">
              Message
            </label>
            <textarea
              id="message"
              rows={6}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            ></textarea>
          </div>
          <button
            type="submit"
            className="px-6 py-3 bg-gradient-to-r from-blue-600 to-blue-500 text-white rounded-lg hover:from-blue-700 hover:to-blue-600 flex items-center gap-2"
          >
            <Send className="h-5 w-5" />
            Send Message
          </button>
        </form>
      </section>
    </div>
  );
}