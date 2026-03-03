import { Book, FileText, Video, Download, Search, BookOpen } from 'lucide-react';

export default function Resources() {
  return (
    <div className="space-y-16">
      {/* Hero Section */}
      <section className="text-center space-y-4 py-16 bg-gradient-to-b from-blue-50 to-white rounded-3xl">
        <h1 className="text-4xl font-bold text-gray-900">Health Resources</h1>
        <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed px-4">
          Access comprehensive health information and educational materials
        </p>
        <div className="max-w-2xl mx-auto mt-8 px-4">
          <div className="relative">
            <Search className="absolute left-4 top-3.5 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search resources..."
              className="w-full pl-12 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>
      </section>

      {/* Resource Categories */}
      <section className="max-w-6xl mx-auto px-4">
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          {[
            {
              icon: Book,
              title: "Health Guides",
              description: "Comprehensive guides on various health topics"
            },
            {
              icon: Video,
              title: "Video Library",
              description: "Educational videos and medical procedures"
            },
            {
              icon: FileText,
              title: "Research Papers",
              description: "Latest medical research and findings"
            },
            {
              icon: Download,
              title: "Downloads",
              description: "Forms, checklists, and patient materials"
            },
            {
              icon: BookOpen,
              title: "Patient Education",
              description: "Educational materials for better health understanding"
            }
          ].map((category) => (
            <div key={category.title} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="bg-blue-50 p-3 rounded-full w-fit mb-4">
                <category.icon className="h-6 w-6 text-blue-600" />
              </div>
              <h3 className="font-semibold text-lg mb-2">{category.title}</h3>
              <p className="text-gray-600">{category.description}</p>
              <button className="mt-4 text-blue-600 font-medium hover:text-blue-700">
                Browse Resources →
              </button>
            </div>
          ))}
        </div>
      </section>

      {/* Featured Resources */}
      <section className="max-w-6xl mx-auto px-4">
        <h2 className="text-2xl font-bold mb-6">Featured Resources</h2>
        <div className="grid md:grid-cols-2 gap-6">
          {[
            {
              title: "Understanding Preventive Care",
              type: "Guide",
              description: "Learn about essential preventive healthcare measures"
            },
            {
              title: "Managing Chronic Conditions",
              type: "Video Series",
              description: "Expert advice on chronic disease management"
            },
            {
              title: "Healthy Living Guidelines",
              type: "PDF",
              description: "Comprehensive guide to maintaining a healthy lifestyle"
            },
            {
              title: "Mental Health Awareness",
              type: "Interactive Guide",
              description: "Resources for mental health and well-being"
            }
          ].map((resource) => (
            <div key={resource.title} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <span className="text-sm text-blue-600 font-medium">{resource.type}</span>
              <h3 className="font-semibold text-lg mt-2 mb-2">{resource.title}</h3>
              <p className="text-gray-600 mb-4">{resource.description}</p>
              <button className="text-blue-600 hover:text-blue-700 font-medium">
                Read More →
              </button>
            </div>
          ))}
        </div>
      </section>

      {/* Newsletter Signup */}
      <section className="bg-blue-50 rounded-3xl p-8">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-2xl font-bold mb-4">Stay Updated</h2>
          <p className="text-gray-600 mb-6">
            Subscribe to our newsletter for the latest health resources and updates
          </p>
          <div className="flex gap-4 max-w-md mx-auto">
            <input
              type="email"
              placeholder="Enter your email"
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <button className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
              Subscribe
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}