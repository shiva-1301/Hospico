interface Props {
  isVisible: boolean;
}

const stats = [
  { value: '500+', label: 'Healthcare Partners' },
  { value: '50k+', label: 'Monthly Patients' },
  { value: '98%', label: 'Patient Satisfaction' },
  { value: '24/7', label: 'Support Available' }
];

export default function StatsSection({ isVisible }: Props) {
  return (
    <section className="bg-gradient-to-r from-blue-600 to-blue-400 py-16">
      <div className="max-w-7xl mx-auto px-4">
        <div className="grid md:grid-cols-4 gap-8">
          {stats.map((stat, index) => (
            <div
              key={stat.label}
              className={`text-center text-white transition-all duration-1000 ${
                isVisible
                  ? 'opacity-100 translate-y-0'
                  : 'opacity-0 translate-y-8'
              }`}
              style={{ transitionDelay: `${index * 200}ms` }}
            >
              <div className="text-4xl font-bold mb-2">{stat.value}</div>
              <div className="text-blue-100">{stat.label}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}